"""Validate mod resources and built JARs without starting Minecraft (Python 3.11+)."""

import argparse
import hashlib
import json
import re
import sys
import tomllib
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODULES = {
    '1.16.5': ROOT / 'DragonLoot-1.16.5-forge/DragonLoot-1.16',
    '1.18.2': ROOT / 'DragonLoot-1.18.2-forge/DragonLoot-1.18',
    '1.19.2': ROOT / 'DragonLoot-1.19.2-forge/DragonLoot-1.19',
    '1.20.1': ROOT / 'DragonLoot-1.20.1-forge/DragonLoot-1.20',
    '1.21.1': ROOT / 'DragonLoot-1.21.1-neoforge/DragonLoot-1.21',
    '26.1.2': ROOT / 'DragonLoot-26.1.2-neoforge/DragonLoot-26.1.2',
}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f'duplicate JSON key: {key}')
        result[key] = value
    return result


def invalid_constant(value):
    raise ValueError(f'non-finite JSON number: {value}')


def read_json(raw):
    return json.loads(raw, object_pairs_hook=unique_object, parse_constant=invalid_constant)


def validate_module(module):
    resources = module / 'src/main/resources'
    count = 0
    for path in sorted(resources.rglob('*.json')):
        data = read_json(path.read_text(encoding='utf-8'))
        count += 1
        if path.name.endswith('.mixins.json'):
            for section in ('mixins', 'client', 'server'):
                for name in data.get(section, []):
                    source = module / 'src/main/java' / (data['package'] + '.' + name).replace('.', '/')
                    if not source.with_suffix('.java').is_file():
                        raise ValueError(f'{path}: missing mixin {name}')
        if (path.is_relative_to(resources / 'assets/dragonloot/models')
                or path.is_relative_to(resources / 'assets/dragonloot/items')):
            check_model_refs(data, resources, path)
    for path in resources.rglob('*.toml'):
        tomllib.loads(path.read_text(encoding='utf-8'))
    read_json((resources / 'pack.mcmeta').read_text(encoding='utf-8'))
    return count


def check_model_refs(data, resources, source):
    if isinstance(data, dict):
        for key, value in data.items():
            if key in ('parent', 'model') and isinstance(value, str) and value.startswith('dragonloot:'):
                target = resources / 'assets/dragonloot/models' / (value.split(':', 1)[1] + '.json')
                if not target.is_file():
                    raise ValueError(f'{source}: missing model parent {value}')
            if key == 'textures' and isinstance(value, dict):
                for texture in value.values():
                    if isinstance(texture, str) and texture.startswith('dragonloot:'):
                        target = resources / 'assets/dragonloot/textures' / (texture.split(':', 1)[1] + '.png')
                        if not target.is_file():
                            raise ValueError(f'{source}: missing texture {texture}')
            check_model_refs(value, resources, source)
    elif isinstance(data, list):
        for value in data:
            check_model_refs(value, resources, source)


def validate_jar(jar, module, version):
    with zipfile.ZipFile(jar) as archive:
        if archive.testzip():
            raise ValueError('JAR has a corrupt ZIP member')
        names = archive.namelist()
        if len(names) != len(set(names)):
            raise ValueError('JAR has duplicate entries')
        for path in (module / 'src/main/resources').rglob('*'):
            if not path.is_file():
                continue
            name = path.relative_to(module / 'src/main/resources').as_posix()
            actual = archive.read(name)
            if name.endswith('.toml'):
                metadata = tomllib.loads(actual.decode('utf-8'))
                if '${version}' in metadata['mods'][0]['version']:
                    raise ValueError('JAR mod version was not expanded')
            elif actual != path.read_bytes():
                raise ValueError(f'JAR has stale resource: {name}')
        mixins = read_json(archive.read('dragonloot.mixins.json'))
        for section in ('mixins', 'client', 'server'):
            for name in mixins.get(section, []):
                class_name = (mixins['package'] + '.' + name).replace('.', '/') + '.class'
                if class_name not in names:
                    raise ValueError(f'JAR lacks mixin class: {class_name}')
        # These NeoForge ports use official names in development and production.
        # Older Forge ports still need their declared mapping file.
        if 'refmap' in mixins:
            refmap = read_json(archive.read(mixins['refmap']))
            if not isinstance(refmap, dict):
                raise ValueError('invalid mixin refmap')
        elif version not in ('1.21.1', '26.1.2'):
            raise ValueError('Forge mixin config must declare its refmap')
        if version not in ('1.21.1', '26.1.2'):
            manifest = archive.read('META-INF/MANIFEST.MF').decode('utf-8').replace('\r\n ', '').replace('\n ', '')
            attributes = dict(line.split(': ', 1) for line in manifest.splitlines() if ': ' in line)
            if 'dragonloot.mixins.json' not in attributes.get('MixinConfigs', '').split(','):
                raise ValueError('Forge JAR must register its mixin config in the manifest')
        for source_set in ('gametest', 'gameTest', 'smokeTest'):
            for source in (module / f'src/{source_set}/java').rglob('*.java'):
                class_name = source.relative_to(module / f'src/{source_set}/java').with_suffix('').as_posix()
                if any(name == class_name + '.class' or name.startswith(class_name + '$') for name in names):
                    raise ValueError(f'release JAR includes development regression code: {class_name}')
        major = int.from_bytes(archive.read('net/dragonloot/DragonLootMain.class')[6:8], 'big')
        expected = 69 if version == '26.1.2' else 65 if version == '1.21.1' else 52 if version == '1.16.5' else 61
        if major != expected:
            raise ValueError(f'wrong Java class version: {major}, expected {expected}')
        if not any(name.startswith('LICENSE') for name in names):
            raise ValueError('JAR lacks the license')
        return metadata['mods'][0]['version']


def stage_prism(output, jar, module, mod_version):
    if output.exists():
        raise ValueError(f'output already exists; choose a new filename: {output}')
    properties = dict(re.findall(r'^([\w.]+)=(.*)$', (module / 'gradle.properties').read_text(), re.MULTILINE))
    minecraft_version = properties['minecraft_version'].strip()
    neo_key = 'neoforge_version' if 'neoforge_version' in properties else 'neo_version' if 'neo_version' in properties else None
    loader = 'neoforge' if neo_key else 'forge'
    loader_uid = 'net.neoforged' if loader == 'neoforge' else 'net.minecraftforge'
    loader_version = properties[neo_key if loader == 'neoforge' else 'forge_version'].strip()
    pack = {'formatVersion': 1, 'components': [
        {'uid': 'net.minecraft', 'version': minecraft_version, 'important': True},
        {'uid': loader_uid, 'version': loader_version},
    ]}
    config = '\n'.join([
        '[General]', 'InstanceType=OneSix', f'name=DragonLoot {mod_version} {minecraft_version} test',
        'iconKey=default', 'OverrideMemory=true', 'MinMemAlloc=512', 'MaxMemAlloc=2048',
        'OverrideJavaLocation=true', 'AutomaticJava=true', 'OverrideConsole=true',
        'ShowConsole=true', 'ShowConsoleOnError=true', 'AutoCloseConsole=false', '',
    ])
    mod_name = f'dragonloot-{mod_version}-{minecraft_version}-{loader}.jar'
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'x', compression=zipfile.ZIP_DEFLATED) as archive:
        archive.writestr('mmc-pack.json', json.dumps(pack, indent=2) + '\n')
        archive.writestr('instance.cfg', config)
        archive.write(jar, '.minecraft/mods/' + mod_name)
        archive.writestr('SHA256.txt', hashlib.sha256(jar.read_bytes()).hexdigest() + '  ' + mod_name + '\n')
        for document in ('TESTING.md', 'PROJECTILE-TESTS.md',
                         'CONFIG-PROFILE-TESTS.md', 'COMPATIBILITY-TESTS.md',
                         'BUILD-REPRODUCIBILITY.md', 'NETWORK-CONTROLS.md'):
            path = ROOT / 'docs' / document
            if path.is_file():
                archive.write(path, document)
    print(f'Prepared Prism import archive: {output} (includes contributor testing guides)')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--version', choices=MODULES, help='default: validate all resource trees')
    parser.add_argument('--jar', type=Path, help='also verify this built JAR; requires --version')
    parser.add_argument('--prism-output', type=Path, help='prepare a version-specific Prism import ZIP; requires --jar')
    args = parser.parse_args()
    if args.jar and not args.version:
        parser.error('--jar requires --version')
    if args.prism_output and not args.jar:
        parser.error('--prism-output requires --jar and --version')
    try:
        selected = {args.version: MODULES[args.version]} if args.version else MODULES
        for version, module in selected.items():
            count = validate_module(module)
            print(f'{version}: validated {count} JSON resources, mixin sources, model references and metadata')
        if args.jar:
            mod_version = validate_jar(args.jar, MODULES[args.version], args.version)
            print(f'{args.jar.name}: verified resources, mixin packaging, Java target and license')
            if args.prism_output:
                stage_prism(args.prism_output, args.jar, MODULES[args.version], mod_version)
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        print(f'Validation failed: {error}', file=sys.stderr)
        return 1
    return 0


if __name__ == '__main__':
    sys.exit(main())
