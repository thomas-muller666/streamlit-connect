import logging
import uuid

import streamlit as st

from app.logging_config import LoggingConfig
from core.container_context import ContainerContext

# List of radio button options
options = ['Option 1', 'Option 2', 'Option 3']


# Callback function to update session state with the selected index
def update_index():
    selected_option = st.session_state.selected_option
    st.session_state.selected_index = options.index(selected_option)
    print(f"Selected Index: {st.session_state.selected_index}")
    print(f"Selected Value: {selected_option}")


def main():
    session_id = get_or_create_session_id()
    log = logging.getLogger("streamlit-grpc")

    # Initialize session state if not already set
    if 'selected_option' not in st.session_state:
        st.session_state.selected_option = options[0]
        st.session_state.selected_index = 0

    # Create the radio button with a callback
    st.radio("Choose an option:",
             options,
             index=st.session_state.selected_index,
             key='selected_option',
             on_change=update_index)

    # Display the current value and index
    st.write(f"Selected Value: {st.session_state.selected_option}")
    st.write(f"Selected Index: {st.session_state.selected_index}")

    root = ContainerContext(st)
    sidebar = ContainerContext(st.sidebar)
    container = ContainerContext(st.container(border=True))
    container2 = ContainerContext(st.container(border=True))
    container3 = container2.container(border=True)
    expander = container2.expander("Expander")

    root.call_func("header", "Hello from main")
    sidebar.call_func("title", "Hello from sidebar")
    container.call_func("subheader", "Hello from container")
    container2.call_func("subheader", "Hello from container2")
    container3.call_func("subheader", "Hello from container3")
    expander.call_func("subheader", "Hello from expander")


def init():
    LoggingConfig.configure()


def get_or_create_session_id() -> str:
    if "session_id" not in st.session_state or st.session_state.session_id is None:
        init()
        session_id = str(uuid.uuid4())
        st.session_state.session_id = session_id
    else:
        session_id = st.session_state.session_id
    return session_id


if __name__ == '__main__':
    main()
