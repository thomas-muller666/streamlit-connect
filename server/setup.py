from setuptools import setup, find_packages

setup(
    name='your_project_name',
    version='0.1.SNAPSHOT',
    description='Middleman server between a remote (gRPC) StreamlitConnect application and a Streamlit frontend.',
    author='Thomas Muller',
    author_email='me.thomas.muller@gmail.com',
    packages=find_packages(where='src'),  # Look for packages inside src
    package_dir={'': 'src'},              # Root package directory is src
    install_requires=[
        'streamlit>=1.37.0',
        'requests>=2.24.0',
        'protobuf>=5.27.0',
    ],
    entry_points={
        'console_scripts': [
            'run-app=app.main:main',  # Custom command to run your Streamlit app
        ],
    },
)
