@echo off
setlocal

:: Define the source and target directories
set PROTO_DIR=..\proto
set OUTPUT_DIR=src\proto

:: Define the path to the Python script
set PYTHON_SCRIPT=fix_imports.py

:: Check if the target directory exists
if exist %OUTPUT_DIR% (
    echo Deleting existing files in %OUTPUT_DIR%...
    rd /s /q %OUTPUT_DIR%
)

:: Recreate the directory
echo Creating %OUTPUT_DIR%...
mkdir %OUTPUT_DIR%

:: Define the .proto files to compile
set PROTO_FILES=actions.proto commons.proto navigation.proto operations.proto pingpong.proto

:: Generate the gRPC code
echo Generating gRPC code...
for %%f in (%PROTO_FILES%) do (
    python -m grpc_tools.protoc --proto_path=%PROTO_DIR% --python_out=%OUTPUT_DIR% --grpc_python_out=%OUTPUT_DIR% %PROTO_DIR%\%%f
)

:: Create the __init__.py file to make it a package
echo Creating __init__.py...
type nul > %OUTPUT_DIR%\__init__.py

:: Run the Python script to fix the imports
echo Fixing imports...
python %PYTHON_SCRIPT%

echo gRPC code generation complete.

endlocal
