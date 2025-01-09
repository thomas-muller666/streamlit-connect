from google.protobuf.message import Message


class ValidationError(ValueError):
    def __init__(self, message_name, missing_field):
        super().__init__(f"Missing required field '{missing_field}' in message '{message_name}'.")


def validate_message(message: Message, field_name: str) -> None:
    """
    Validates if a specific field is present in a protobuf message.

    Args:
      message: The protobuf message to validate.
      field_name: The name of the field to check.

    Raises:
      ValidationError: If the specified field is missing from the message.
    """
    if not message.HasField(field_name):
        raise ValidationError(message.__class__.__name__, field_name)
