import glob
import logging.config
import os
from io import StringIO

from utils.interpolator import interpolate


class LoggingConfig:

    @staticmethod
    def configure():
        """
        Main function to configure logging.
        """
        print('Configuring logging...')

        # Step 1: Prepare the log directory
        log_dir = LoggingConfig._prepare_log_directory()

        # Check if log directory was created successfully
        if not log_dir or not os.path.exists(log_dir):
            raise RuntimeError(f"Failed to create or access log directory: {log_dir}")

        # Step 2: Clean up old log files
        LoggingConfig._cleanup_old_logs(log_dir)

        # Step 3: Interpolate placeholders in the logging configuration file
        config_stream = LoggingConfig._interpolate_logging_config(log_dir)

        # Step 4: Configure logging using the interpolated configuration
        LoggingConfig._apply_logging_config(config_stream)

        print('Logging configured.')

    @staticmethod
    def _prepare_log_directory():
        """
        Prepare the logging directory in the parent folder of "src".
        """
        # Navigate up two levels from the current file to the parent of "src"
        project_root = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

        # Create the log directory at the parent level of "src"
        log_dir = os.path.join(project_root, 'log')
        log_dir = os.path.normpath(log_dir)  # Normalize the path for the OS

        if not os.path.exists(log_dir):
            print(f'Creating log directory at: {log_dir}')
            os.makedirs(log_dir)

        print(f"Log directory set to: {log_dir}")
        return log_dir

    @staticmethod
    def _cleanup_old_logs(log_dir):
        """
        Remove old log files in the log directory.
        """
        files = glob.glob(os.path.join(log_dir, '*.log'))
        for f in files:
            try:
                os.remove(f)
            except OSError as e:
                print(f"Error: {f} : {e.strerror}")

    @staticmethod
    def _interpolate_logging_config(log_dir):
        """
        Interpolate placeholders in the logging configuration file and return the interpolated content
        as an in-memory file-like object.
        """
        config_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'logging_config.ini')

        # Replace the placeholder for log file path with the actual path
        log_file_path = os.path.join(log_dir, 'app.log')
        log_file_path = os.path.normpath(log_file_path)  # Normalize the path for the OS

        # Ensure log_file_path is treated as a raw string to avoid issues with escape sequences
        replacements = {
            'log_file_path': log_file_path.replace('\\', '\\\\'),  # Double backslashes to prevent escape sequences
        }

        # Open the original logging config file for reading
        with open(config_path, 'r') as input_file:
            # Use StringIO to capture the interpolated output
            interpolated_stream = StringIO()

            # Interpolate placeholders in the logging configuration file
            interpolate(input_file, interpolated_stream, replacements)

            # Reset the stream position to the beginning
            interpolated_stream.seek(0)

        return interpolated_stream

    @staticmethod
    def _apply_logging_config(config_stream):
        """
        Apply the logging configuration from the in-memory file-like object.
        """
        logging.config.fileConfig(config_stream, disable_existing_loggers=False)
