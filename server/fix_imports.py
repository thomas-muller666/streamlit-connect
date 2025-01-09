import os
import re


def fix_imports(dir: str):
    for filename in os.listdir(dir):
        if filename.endswith('.py'):
            path = os.path.join(dir, filename)
            with open(path, 'r+') as file:
                data = file.read()
                file.seek(0)
                file.write(re.sub(r'import (.*)_pb2 as (.*__pb2)', r'from . import \1_pb2 as \2', data))
                file.truncate()


if __name__ == '__main__':
    fix_imports('src/proto')
