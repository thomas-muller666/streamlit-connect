import logging
import os
import sys
from io import StringIO

import streamlit as st
import toml

# Calculate the path to the src directory
current_dir = os.path.dirname(os.path.abspath(__file__))
src_dir = os.path.join(current_dir, '..')

# Add the src directory to sys.path
sys.path.insert(0, src_dir)

from core.remote_streamlit_client import RemoteStreamlitClient
from utils.interpolator import interpolate
from logging_config import LoggingConfig


def main():
    client = _get_or_create_client()
    try:
        client.render()
    except Exception as e:
        logging.getLogger("streamlit_connect").error(f"Error rendering: {e}")
        st.error(f"Error rendering: {str(e)}")
        st.error(f"Please check the logs for more information.")
        st.exception(e)


def _init():
    LoggingConfig.configure()


def _get_or_create_client() -> RemoteStreamlitClient:
    if "client" not in st.session_state or st.session_state.client is None:
        _init()
        config = _read_toml_to_dict('config.toml')
        client = RemoteStreamlitClient(config)
        st.session_state.client = client
        logging.getLogger("streamlit_connect").debug(f"Created client: {client} with session id: {client.session_id}")
    else:
        client = st.session_state.client
    return client


def _read_toml_to_dict(filename):
    """
    Reads a TOML file, interpolates placeholders, and returns the configuration as a dictionary.
    This is done entirely in-memory without writing to a temporary file.
    """
    # Get the path of the directory where the current script is located
    current_script_dir = os.path.dirname(os.path.realpath(__file__))

    # Construct full file path from the script directory and filename
    file_path = os.path.join(current_script_dir, filename)

    # Open the original TOML config file for reading
    with open(file_path, 'r') as input_file:
        # Use StringIO to capture the interpolated output
        interpolated_stream = StringIO()

        # Interpolate placeholders in the TOML configuration file
        interpolate(input_file, interpolated_stream, replacements=None)

        # Reset the stream position to the beginning
        interpolated_stream.seek(0)

        # Load the interpolated TOML configuration from the in-memory stream
        data = toml.load(interpolated_stream)

    return data


if __name__ == '__main__':
    main()
