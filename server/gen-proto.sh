#!/bin/bash

# Define the source and target directories
PROTO_DIR="../proto"
OUTPUT_DIR="src/streamlit_grpc"

# Define the path to the Python script
PYTHON_SCRIPT="fix_imports.py"

# Check if the target directory exists
if [ -d "$OUTPUT_DIR" ]; then
    echo "Deleting existing files in $OUTPUT_DIR..."
    rm -r "$OUTPUT_DIR"
fi

# Create the directory
echo "Creating $OUTPUT_DIR..."
mkdir -p "$OUTPUT_DIR"

# Define the .proto files to compile
PROTO_FILES="actions.proto commons.proto navigation.proto operations.proto pingpong.proto"

# Generate the gRPC code
echo "Generating gRPC code..."
for file in $PROTO_FILES; do
    python -m grpc_tools.protoc --proto_path=$PROTO_DIR --python_out=$OUTPUT_DIR --grpc_python_out=$OUTPUT_DIR "$PROTO_DIR/$file"
done

# Create the __init__.py file to make it a package
echo "Creating __init__.py..."
touch "$OUTPUT_DIR/__init__.py"

# Run the Python script to fix the imports
echo "Fixing imports..."
python $PYTHON_SCRIPT

echo "gRPC code generation complete."