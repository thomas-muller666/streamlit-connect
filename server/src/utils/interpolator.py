import os
import re
import shutil
import tempfile


def interpolate(input_stream, output_stream, replacements: dict = None):
    """
    Replaces all instances of ${key} or ${key:default} in the input stream with the corresponding values
    from the replacements dictionary. If a key is not found in the dictionary, it looks up the key
    (case-insensitively) in the environment variables. If neither is found, it uses the default value if provided.

    :param input_stream: The input stream (e.g., file-like object or StringIO)
    :param output_stream: The output stream (e.g., file-like object or StringIO)
    :param replacements: Dictionary containing the replacement values for placeholders or None to use only environment variables
    """

    def replacer(match):
        # Extract key and default value from the match
        full_placeholder = match.group(0)
        key = match.group(1).lower()  # Convert key to lowercase for case-insensitivity
        default = match.group(2) if match.group(2) else ''

        # Determine the replacement value
        replacement = None
        if replacements is not None:
            # Attempt to find the key case-insensitively in the replacements dict
            for dict_key in replacements:
                if dict_key.lower() == key:
                    replacement = str(replacements[dict_key])
                    break

        if replacement is None:
            # Fallback to environment variables if the key is not found in the replacements dictionary
            replacement = os.environ.get(key.upper(), os.environ.get(key.lower(), default))

        # Log the replacement process
        print(f"Replacing {full_placeholder} with {replacement}")

        return replacement

    # Read the entire input
    content = input_stream.read()

    # Regular expression to match ${key} and ${key:default}
    pattern = r"\$\{(\w+)(?::([^}]*))?\}"

    # Replace using the replacer function
    result = re.sub(pattern, replacer, content)

    # Write the modified content to the output stream
    output_stream.write(result)


def interpolate_file(file_path, replacements: dict = None):
    """
    Reads the content of a file, interpolates all placeholders in the content, and writes the result back to the file.
    This function uses a temporary file to ensure that the original file is not overwritten until the process is
    complete.

    :param file_path: Path to the file to be interpolated
    :param replacements: Dictionary containing the replacement values for placeholders or None to use environment variables
    """

    # Create a temporary file
    temp_file_path = None
    with tempfile.NamedTemporaryFile(delete=False) as temp_file:
        temp_file_path = temp_file.name
        with open(file_path, 'r') as input_file:
            interpolate(input_file, temp_file, replacements)

    # Replace the original file with the temporary file
    shutil.move(temp_file_path, file_path)
