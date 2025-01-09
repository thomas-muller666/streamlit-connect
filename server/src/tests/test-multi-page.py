import logging
import types
import uuid

import streamlit as st

from app.logging_config import LoggingConfig

# List of radio button options
options = ['Option 1', 'Option 2', 'Option 3']


class MultiPageApp:
    def __init__(self, session_id):
        self.session_id = session_id
        pass

    def get_page(self, name):
        st.title("Page: " + name)


def main():
    log = logging.getLogger("streamlit-grpc")
    app = get_or_create_app()
    page1_func = create_dynamic_get_page_function(app, "page1")
    page2_func = create_dynamic_get_page_function(app, "page2")
    pg = st.navigation([
        st.Page(page1_func, title="Page 1"),
        st.Page(page2_func, title="Page 2")
    ])
    pg.run()


def init():
    LoggingConfig.configure()


def get_or_create_app() -> MultiPageApp:
    if "app" not in st.session_state or st.session_state.app is None:
        init()
        app = MultiPageApp(str(uuid.uuid4()))
        st.session_state.app = app
    else:
        app = st.session_state.app
    return app


def create_dynamic_get_page_function(self, page):
    prefix = "get_"
    func_name = f"{prefix}{page}"

    if hasattr(self, func_name):
        return getattr(self, func_name)

    code = f"""
def {func_name}(self):
    import inspect
    current_frame = inspect.currentframe()
    func_name = current_frame.f_code.co_name
    # Extract the page from the method name by stripping the 'get_' prefix
    extracted_page = func_name[len('{prefix}'):]
    self.get_page(extracted_page)    
"""
    local_context = {}
    exec(code, {}, local_context)

    # Get the function from local_context
    func = local_context[func_name]

    # Bind the new function to the current instance
    bound_func = types.MethodType(func, self)

    # Set the new function as an attribute of the instance
    setattr(self, func_name, bound_func)

    # Return a pointer to the new function
    return bound_func


if __name__ == '__main__':
    main()
