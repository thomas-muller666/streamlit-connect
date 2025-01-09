import logging
import queue
import types
import uuid
from datetime import datetime, date, timedelta
from typing import Dict

import streamlit as st

import proto.actions_pb2 as actions_proto
import proto.commons_pb2 as commons_proto
import proto.navigation_pb2 as nav_proto
import proto.operations_pb2 as ops_proto
from core.container_context import ContainerContext
from core.enums import ColumnGap, ColumnVerticalAlignment
from core.grpc_client import GrpcClient
from utils.session_logger_adapter import SessionLoggerAdapter


class RemoteStreamlitClient:
    # Only one instance of this client is created per session

    def __init__(self, config: Dict):
        self._session_id = str(uuid.uuid4())
        self._log = SessionLoggerAdapter(logging.getLogger("streamlit_connect"), {"session_id": self._session_id[-7:]})
        self._request_actions = []
        host = config.get('host', 'localhost')
        port = config.get('port', '50051')
        self._grpc_client = GrpcClient(f'{host}:{port}', self._log)
        self._reset_contexts()
        self._app = st.query_params.get("app", None)
        self._streams = {}
        self._seq = 0

    @property
    def session_id(self):
        return self._session_id

    def render(self):
        self._log.debug(f"Rendering app (using gRPC) for session: {self._session_id}")

        self._seq += 1
        self._log.debug(f"Sequence number: {self._seq}")

        # Get the navigation menu (if any)
        self._run_nav()

    def _run_nav(self):
        streamlit_request = nav_proto.StreamlitNavigationRequest(
            session_id=self._session_id,
            seq=self._seq,
            app=self._app,
            actions=self._request_actions
        )
        self._log.debug(f"Sending navigation request to remote gRPC server: {streamlit_request}")

        navigation_resp = self._grpc_client.get_navigation(streamlit_request)
        self._log.debug(f"Received navigation response: {navigation_resp}")

        # Create dictionary of menu items
        menu_items = {}

        if navigation_resp.items is not None:
            for item in navigation_resp.items:
                menu_items[item.header] = [  # Header might be None
                    st.Page(
                        page=self._get_or_create_dynamic_get_page_function(entry.page),
                        title=entry.title,
                        icon=entry.icon if entry.icon else None,
                        default=entry.is_default
                    )
                    for entry in item.entries
                ]

        if len(menu_items) == 0:
            self._log.debug("No navigation menu items found.")
            self._run_ops(page=None)  # No navigation, run the operations with default page
        else:
            # if menu_items has only one item with key = None, flatten to a list
            if len(menu_items) == 1 and None in menu_items:
                self._log.debug("Flattening menu items to a list,   ")
                menu_items = menu_items[None]

            position = nav_proto.StreamlitNavigation.Location.Name(navigation_resp.location).lower() \
                if navigation_resp.location is not None else "sidebar"

            self._log.debug(f"Creating navigation menu with items: {menu_items} at position: {position}")

            pg = st.navigation(menu_items, position=position)
            pg.run()

        self._request_actions = []
        self._log.debug(f"Navigation done for session: {self._session_id}")

    # This is also called from a dynamic function when a navigation menu is defined
    def _run_ops(self, page):
        self._log.debug(f"Requesting operations for page: {page}")

        streamlit_request = ops_proto.StreamlitOperationsRequest(
            session_id=self._session_id,
            seq=self._seq,
            app=self._app,
            page=page,
            actions=self._request_actions
        )

        operations = self._grpc_client.get_operations(streamlit_request)

        for operation in operations:
            streamlit_op_type = operation.WhichOneof('operation')

            self._log.debug(f"Operation: {operation}")

            self._handle_streamlit_operation(streamlit_op_type, operation)
            if streamlit_op_type == 'endOp':
                break

        self._post_op_cleanup()
        self._log.debug(f"App (re)run ended for session: {self._session_id}")

    def _get_or_create_dynamic_get_page_function(self, page):
        """
        :param page: The page name for which the dynamic function is being created.
        :return: The dynamically created function.

        This method creates a dynamic function for a given page name to be used as a callback for a Page object in
        st.navigation. This is a work-around for the limitation of not being able to pass arguments to the callback.
        """
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
    
    # Run the operations for the extracted page
    self._run_ops(extracted_page)    
"""
        local_context = {}
        exec(code, {}, local_context)

        # Get the function from local_context
        func = local_context[func_name]

        # Bind the new function to the current instance
        bound_func = types.MethodType(func, self)

        # Set the new function as an attribute of the instance
        setattr(self, func_name, bound_func)

        self._log.debug(f"Created dynamic function: {func_name}")

        # Return a pointer to the new function
        return bound_func

    def _post_op_cleanup(self):
        self._log.debug("Cleaning up...")
        self._request_actions = []
        self._reset_contexts()

    def _reset_contexts(self):
        self._contexts = {"root": ContainerContext(st), "sidebar": ContainerContext(st.sidebar)}

    def _get_context(self, key='root'):
        self._log.debug(f"Getting context with key: {key}")
        if key is None:
            key = 'root'
        if key not in self._contexts:
            raise KeyError(f"No context found with key {key} in contexts.")
        context = self._contexts[key]
        self._log.debug(f"Using context for key {key}: {context}")
        return context

    def _save_context(self, key, context):
        self._log.debug(f"Saving context: {key} -> {context}")
        self._contexts[key] = context

    def _handle_streamlit_operation(self, streamlit_operation_type, operation):

        if streamlit_operation_type == 'innerContainerOp':
            self._handle_inner_container_operation(operation.innerContainerOp)

        elif streamlit_operation_type == 'expandableContainerOp':
            self._handle_expandable_container_operation(operation.expandableContainerOp)

        elif streamlit_operation_type == 'placeholderContainerOp':
            self._handle_placeholder_container_operation(operation.placeholderContainersOp)

        elif streamlit_operation_type == 'tabContainersOp':
            self._handle_tab_containers_operation(operation.tabContainersOp)

        elif streamlit_operation_type == 'columnContainersOp':
            self._handle_column_containers_operation(operation.columnContainersOp)

        elif streamlit_operation_type == 'titleOp':
            self._handle_title_operation(operation.titleOp)

        elif streamlit_operation_type == 'headerOp':
            self._handle_header_operation(operation.headerOp)

        elif streamlit_operation_type == 'subheaderOp':
            self._handle_subheader_operation(operation.subheaderOp)

        elif streamlit_operation_type == 'captionOp':
            self._handle_caption_operation(operation.captionOp)

        elif streamlit_operation_type == 'codeOp':
            self._handle_code_operation(operation.codeOp)

        elif streamlit_operation_type == 'dividerOp':
            self._handle_divider_operation(operation.dividerOp)

        elif streamlit_operation_type == 'latexOp':
            self._handle_latex_operation(operation.latexOp)

        elif streamlit_operation_type == 'textOp':
            self._handle_text_operation(operation.textOp)

        elif streamlit_operation_type == 'markdownOp':
            self._handle_markdown_operation(operation.markdownOp)

        elif streamlit_operation_type == 'writeStreamChunkOp':
            self._handle_write_stream_chunk_operation(operation.writeStreamChunkOp)

        elif streamlit_operation_type == 'buttonOp':
            self._handle_button_operation(operation.buttonOp)

        elif streamlit_operation_type == 'checkboxOp':
            self._handle_checkbox_operation(operation.checkboxOp)

        elif streamlit_operation_type == 'toggleOp':
            self._handle_toggle_operation(operation.toggleOp)

        elif streamlit_operation_type == 'radioOp':
            self._handle_radio_operation(operation.radioOp)

        elif streamlit_operation_type == 'selectboxOp':
            self._handle_selectbox_operation(operation.selectboxOp)

        elif streamlit_operation_type == 'multiselectOp':
            self._handle_multiselect_operation(operation.multiselectOp)

        elif streamlit_operation_type == 'selectSliderOp':
            self._handle_select_slider_operation(operation.selectSliderOp)

        elif streamlit_operation_type == 'sliderOp':
            self._handle_slider_operation(operation.sliderOp)

        elif streamlit_operation_type == 'dateInputOp':
            self._handle_date_input_operation(operation.dateInputOp)

        elif streamlit_operation_type == 'timeInputOp':
            self._handle_time_input_operation(operation.timeInputOp)

        elif streamlit_operation_type == 'numberInputOp':
            self._handle_number_input_operation(operation.numberInputOp)

        elif streamlit_operation_type == 'textInputOp':
            self._handle_text_input_operation(operation.textInputOp)

        elif streamlit_operation_type == 'pageLinkOp':
            self._handle_page_link_operation(operation.pageLinkOp)

        elif streamlit_operation_type == 'rerunOp':
            self._handle_rerun_operation(operation.rerunOp)

        elif streamlit_operation_type == 'stopOp':
            self._handle_stop_operation(operation.stopOp)

        elif streamlit_operation_type == 'endOp':
            self._handle_end_operation(operation.endOp)

        elif streamlit_operation_type == 'terminateSessionOp':
            self._handle_terminate_session_operation(operation.terminateSessionOp)

        elif streamlit_operation_type == 'switchPageOp':
            self._handle_switch_page_operation(operation.terminateSessionOp)

    def _handle_title_operation(self, operation: ops_proto.TitleOp):
        context = self._get_context(operation.container)
        context.call_func("title", operation.body, operation.anchor, help=operation.help)

    def _handle_header_operation(self, operation: ops_proto.HeaderOp):
        context = self._get_context(operation.container)
        context.call_func("header", operation.body, operation.anchor, help=operation.help, divider=operation.divider)

    def _handle_subheader_operation(self, operation: ops_proto.SubheaderOp):
        context = self._get_context(operation.container)
        context.call_func("subheader", operation.body, operation.anchor, help=operation.help, divider=operation.divider)

    def _handle_caption_operation(self, operation: ops_proto.CaptionOp):
        context = self._get_context(operation.container)
        context.call_func("caption", operation.body)

    def _handle_code_operation(self, operation: ops_proto.CodeOp):
        context = self._get_context(operation.container)
        context.call_func("code", operation.body, language=operation.language, line_numbers=operation.line_numbers)

    def _handle_divider_operation(self, operation: ops_proto.DividerOp):
        context = self._get_context(operation.container)
        context.call_func("divider")

    def _handle_latex_operation(self, operation: ops_proto.LatexOp):
        context = self._get_context(operation.container)
        context.call_func("latex", operation.body)

    def _handle_text_operation(self, operation: ops_proto.TextOp):
        context = self._get_context(operation.container)
        context.call_func("text", operation.body, help=operation.help)

    def _handle_markdown_operation(self, operation: ops_proto.MarkdownOp):
        context = self._get_context(operation.container)
        context.call_func(
            "markdown",
            operation.body,
            unsafe_allow_html=operation.unsafe_allow_html,
            help=operation.help
        )

    # ------------------------------------------------
    # Write Stream Chunk
    # ------------------------------------------------
    # FIXME: not working as expected, the stream is not being displayed in the browser
    def _handle_write_stream_chunk_operation(self, operation: ops_proto.WriteStreamChunkOp):
        stream_key = operation.key

        if stream_key not in self._streams:
            # Create queue for this stream
            self._streams[stream_key] = queue.Queue()

            # Create generator for this stream
            def generator(queue_obj):
                while True:
                    try:
                        chunk = queue_obj.get(timeout=5)  # 5 seconds timeout for queue get
                        self._log.debug(f"Queue size after get: {queue_obj.qsize()}")
                    except queue.Empty:
                        self._log.debug("The Queue is empty and get() has timed out.")
                        continue

                    if chunk is None:
                        self._log.debug("Stream ended.")
                        break
                    else:
                        self._log.debug(f"Yielding chunk: {chunk}")
                        yield chunk

            # Get the context for the provided container key
            context = self._get_context(operation.container)

            # Start the stream
            self._log.debug("Starting the stream...")
            context.call_func("write_stream", generator(self._streams[stream_key]))
            self._log.debug("stream started")

        # Add chunk to queue
        self._log.debug(f"Adding chunk to queue: {operation.body}")
        self._streams[stream_key].put(operation.body)
        self._log.debug(f"Queue size after chunk added: {self._streams[stream_key].qsize()}")

        if operation.is_last:
            # Yield a final None to signal the end of this stream and then delete the entry from the dictionary
            self._streams[stream_key].put(None)
            del self._streams[stream_key]
            self._log.debug(f"Last chunk processed and stream_key '{stream_key}' removed from _streams dictionary")

    # ------------------------------------------------
    # Button / LinkButton
    # ------------------------------------------------

    # Streamlit executes callbacks before the main script, so we add the request_actions list with a ButtonAction
    def _button_click_callback(self, *args, **kwargs):
        """
        Button callback
        """
        # button key is the first item in args
        key = args[0]
        self._log.debug(f'Button click callback: {key}')

        # Create a ButtonAction message
        button_action = actions_proto.ButtonAction(key=key, args=args[1:], kwargs=kwargs)

        # Create an Action message with the ButtonAction
        action = actions_proto.Action(button_action=button_action)

        # Add the Action to the list of request actions that is sent with the request to the remote server
        self._request_actions.append(action)

    def _handle_button_operation(self, operation: ops_proto.ButtonOp):
        # prepend button.key to the args list
        widget_properties = operation.widget_props
        callback_args = [widget_properties.key]
        context = self._get_context(widget_properties.container)

        if operation.url:
            context.call_func(
                "link_button",
                label=widget_properties.label,
                help=widget_properties.help,
                type='secondary' if operation.type == 1 else ('primary' if operation.type == 0 else 'secondary'),
                disabled=widget_properties.disabled,
                use_container_width=widget_properties.use_container_width,
                url=operation.url
            )
        else:
            clicked = context.call_func(
                "button",
                key=widget_properties.key,
                label=widget_properties.label,
                help=widget_properties.help,
                on_click=self._button_click_callback,
                args=callback_args,
                kwargs={},
                type='secondary' if operation.type == 1 else ('primary' if operation.type == 0 else 'secondary'),
                disabled=widget_properties.disabled,
                use_container_width=widget_properties.use_container_width,
            )
            if clicked:
                self._log.debug(f"Button clicked: {widget_properties.key}")

    # ------------------------------------------------
    # Checkbox
    # ------------------------------------------------

    # Streamlit executes callbacks before the main script, so we add the request_actions list with a CheckboxAction
    def _checkbox_change_callback(self, *args, **kwargs):
        # checkbox key is the first item in args
        key = args[0]
        value: bool = args[1]

        self._log.debug(f"Checkbox changed to '{value}': {key}")

        # Create a CheckboxAction message
        checkbox_action = actions_proto.CheckboxAction(key=key, value=value, args=args[2:], kwargs=kwargs)

        # Create an Action message with the CheckboxAction
        action = actions_proto.Action(checkbox_action=checkbox_action)

        # Add the Action to the list of request actions that is sent with the request to the remote server
        self._request_actions.append(action)

    def _handle_checkbox_operation(self, operation: ops_proto.CheckboxOp):
        # prepend checkbox.key and reversed value to the args list
        widget_properties = operation.widget_props
        callback_args = [widget_properties.key, not operation.value]
        context = self._get_context(widget_properties.container)

        checked = context.call_func(
            "checkbox",
            key=widget_properties.key,
            label=widget_properties.label,
            help=widget_properties.help,
            value=operation.value,
            on_change=self._checkbox_change_callback,
            args=callback_args,  # updated args
            kwargs={},
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
        )
        if checked:
            self._log.debug(f"Checkbox checked: {widget_properties.key}")

    # ------------------------------------------------
    # Toggle
    # ------------------------------------------------

    def _toggle_switch_callback(self, *args, **kwargs):
        key = args[0]
        value: bool = args[1]
        self._log.debug(f"Toggle switeched to '{value}': {key}")
        toggle_action = actions_proto.ToggleAction(key=key, value=value, args=args[2:], kwargs=kwargs)
        action = actions_proto.Action(toggle_action=toggle_action)
        self._request_actions.append(action)

    def _handle_toggle_operation(self, operation: ops_proto.ToggleOp):
        widget_properties = operation.widget_props
        callback_args = [widget_properties.key, not operation.value]
        context = self._get_context(widget_properties.container)

        on = context.call_func(
            "toggle",
            key=widget_properties.key,
            label=widget_properties.label,
            help=widget_properties.help,
            value=operation.value,
            on_change=self._toggle_switch_callback,
            args=callback_args,
            kwargs={},
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
        )
        if on:
            self._log.debug(f"Toggle ON: {widget_properties.key}")
        else:
            self._log.debug(f"Toggle OFF: {widget_properties.key}")

    # ------------------------------------------------
    # Radio
    # ------------------------------------------------

    def _radio_change_callback(self, *args, **kwargs):
        key = args[0]
        options = args[1:]
        selected_option = st.session_state[key]
        index = options.index(selected_option) if selected_option else -1
        kwargs['index'] = str(index)

        self._log.debug(f"Radio '{key}' changed to [{index}]: {selected_option}'")

        radio_action = actions_proto.RadioAction(
            key=key,
            index=index,
            args=args[len(options) + 1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(radio_action=radio_action)
        self._request_actions.append(action)

    def _handle_radio_operation(self, operation: ops_proto.RadioOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        options = list(operation.options)
        index = None if operation.index < 0 or operation.index >= len(options) else operation.index

        # Set the selected option in the session state
        st.session_state[key] = options[index] if index is not None else ""

        callback_args = [key] + options
        context = self._get_context(widget_properties.container)

        selected_option = context.call_func(
            "radio",
            key=key,
            label=widget_properties.label,
            help=widget_properties.help,
            options=options,
            captions=list(operation.captions),
            index=index,
            horizontal=operation.horizontal,
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            on_change=self._radio_change_callback,
            args=callback_args,
            kwargs={},
        )

        self._log.debug(f"Radio '{widget_properties.key}' selected [{index}]: {selected_option}")

    # ------------------------------------------------
    # Selectbox
    # ------------------------------------------------

    def _selectbox_change_callback(self, *args, **kwargs):
        key = args[0]
        options = args[1:]
        selected_option = st.session_state[key]
        index = options.index(selected_option) if selected_option else -1

        self._log.debug(f"Selectbox '{key}' changed to [{index}]: {selected_option}'")

        selectbox_action = actions_proto.SelectboxAction(
            key=key,
            index=index,
            args=args[len(options) + 1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(selectbox_action=selectbox_action)
        self._request_actions.append(action)

    def _handle_selectbox_operation(self, operation: ops_proto.SelectboxOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        options = list(operation.options)
        index = None if operation.index < 0 or operation.index >= len(options) else operation.index

        # Set the selected option in the session state
        st.session_state[key] = options[index] if index is not None else ""

        callback_args = [key] + options
        context = self._get_context(widget_properties.container)

        selected_option = context.call_func(
            "selectbox",
            key=key,
            label=widget_properties.label,
            help=widget_properties.help,
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            on_change=self._selectbox_change_callback,
            args=callback_args,
            kwargs={},
            options=options,
            index=index,
        )

        self._log.debug(f"Selectbox '{widget_properties.key}' selected [{index}]: {selected_option}")

    # ------------------------------------------------
    # Multiselect
    # ------------------------------------------------

    def _multiselect_change_callback(self, *args, **kwargs):
        key = args[0]
        options = args[1:]
        selected_options = st.session_state[key]
        selected_indices = [options.index(option) for option in selected_options if option in options]

        self._log.debug(f"Multiselect '{key}' changed to {selected_indices}: {selected_options}")

        multiselect_action = actions_proto.MultiselectAction(
            key=key,
            selected_indices=selected_indices,
            args=args[len(options) + 1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(multiselect_action=multiselect_action)
        self._request_actions.append(action)

    def _handle_multiselect_operation(self, operation: ops_proto.MultiselectOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        options = list(operation.options)
        selected_indices = [index for index in operation.selected_indices if 0 <= index < len(options)]

        # Set the selected options in the session state
        st.session_state[key] = [options[index] for index in selected_indices]

        callback_args = [key] + options

        context = self._get_context(widget_properties.container)

        selected_options = context.call_func(
            "multiselect",
            key=key,
            label=widget_properties.label,
            help=widget_properties.help,
            disabled=widget_properties.disabled,
            placeholder=operation.placeholder,
            on_change=self._multiselect_change_callback,
            args=callback_args,
            kwargs={},
            options=options,
            default=st.session_state[key],
        )

        self._log.debug(f"Multiselect '{widget_properties.key}' selected {selected_indices}: {selected_options}")

    # ------------------------------------------------
    # SelectSlider
    # ------------------------------------------------

    def _select_slider_change_callback(self, *args, **kwargs):
        key = args[0]
        options = args[1:]
        range_values = st.session_state[key]

        # Check if range_values is a tuple of length 1 or 2, else assume a single value
        if isinstance(range_values, tuple) and len(range_values) == 2:
            lower_value = range_values[0]
            upper_value = range_values[1]
        else:
            lower_value = range_values if not isinstance(range_values, tuple) else range_values[0]
            upper_value = -1

        lower_index = options.index(lower_value) if lower_value in options else -1
        upper_index = options.index(upper_value) if upper_value in options else -1

        self._log.debug(
            f"SelectSlider '{key}' changed to [{lower_value}, {upper_value}] (indices: [{lower_index}, {upper_index}])")

        select_slider_action = actions_proto.SelectSliderAction(
            key=key,
            lower_index=lower_index,
            upper_index=upper_index,
            args=args[len(options) + 1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(select_slider_action=select_slider_action)
        self._request_actions.append(action)

    def _handle_select_slider_operation(self, operation: ops_proto.SelectSliderOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        options = list(operation.options)
        lower_index = None if operation.lower_index < 0 or operation.lower_index >= len(options) else (
            operation.lower_index)
        upper_index = None if operation.upper_index < 0 or operation.upper_index >= len(options) else (
            operation.upper_index)

        selected_options = (options[lower_index], options[upper_index]) \
            if upper_index is not None \
            else (options[lower_index])

        # Set the selected option(s) in the session state
        st.session_state[key] = selected_options

        callback_args = [key] + options

        context = self._get_context(widget_properties.container)

        result_selected_options = context.call_func(
            "select_slider",
            key=key,
            label=widget_properties.label,
            help=widget_properties.help,
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            on_change=self._select_slider_change_callback,
            args=callback_args,
            kwargs={},
            options=options,
            value=selected_options,
        )

        self._log.debug(f"SelectSlider '{widget_properties.key}' selected [{lower_index},{upper_index}]:"
                        f" {result_selected_options}")

    # ------------------------------------------------
    # DateInput
    # ------------------------------------------------

    def _input_date_callback(self, *args, **kwargs):
        key = args[0]
        range_values = st.session_state[key]
        if isinstance(range_values, tuple) and len(range_values) == 2:
            from_date, to_date = range_values
            if from_date > to_date:
                st.warning('From date should not be greater than To date.')
                from_date, to_date = to_date, from_date  # swap the dates
        else:
            from_date = range_values if not isinstance(range_values, tuple) else range_values[0]
            to_date = None

        from_date_str = from_date.strftime('%Y-%m-%d') if from_date else None
        to_date_str = to_date.strftime('%Y-%m-%d') if to_date else None

        self._log.debug(f"DateInput '{key}' changed to [{from_date_str}, {to_date_str}]")

        date_input_action = actions_proto.DateInputAction(
            key=key,
            from_date=from_date_str,
            to_date=to_date_str,
            args=args[1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(date_input_action=date_input_action)
        self._request_actions.append(action)

    def _handle_date_input_operation(self, operation: ops_proto.DateInputOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        format_mapping = {ops_proto.DateFormat.YYYYMMDD: "YYYY/MM/DD",
                          ops_proto.DateFormat.DDMMYYYY: "DD/MM/YYYY",
                          ops_proto.DateFormat.MMDDYYYY: "MM/DD/YYYY"}
        separator_mapping = {ops_proto.DateSeparator.SLASH: "/",
                             ops_proto.DateSeparator.DASH: "-",
                             ops_proto.DateSeparator.DOT: "."}
        format_string = format_mapping[operation.date_format].replace("/", separator_mapping[operation.date_separator])

        min_date = datetime.strptime(operation.min_date, '%Y-%m-%d') if operation.min_date else None
        max_date = datetime.strptime(operation.max_date, '%Y-%m-%d') if operation.max_date else None

        if key not in st.session_state:
            from_date = datetime.strptime(operation.from_date, '%Y-%m-%d') if operation.from_date else None
            to_date = datetime.strptime(operation.to_date, '%Y-%m-%d') if operation.to_date else None

            if operation.today and from_date is None and to_date is None:
                value = date.today()
            elif from_date and to_date:
                value = (from_date, to_date)  # tuple
            elif from_date:
                value = from_date
            else:
                value = None

            st.session_state[key] = value

        value = st.session_state[key]

        self._log.debug(f"DateInput '{key}' value: {value}")

        context = self._get_context(widget_properties.container)
        selected_range = context.call_func(
            "date_input",
            label=widget_properties.label,
            value=value,
            min_value=min_date,
            max_value=max_date,
            key=key,
            help=widget_properties.help,
            on_change=self._input_date_callback,
            args=[key],
            kwargs={},
            format=format_string,
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
        )

        self._log.debug(f"DateInput '{widget_properties.key}' selected: {selected_range}")

    # ------------------------------------------------
    # TimeInput
    # ------------------------------------------------

    def _time_input_callback(self, *args, **kwargs):
        key = args[0]
        value = st.session_state[key]
        value_str = value.strftime('%H:%M:%S') if value else None

        self._log.debug(f"TimeInput '{key}' changed to {value_str}")

        time_input_action = actions_proto.TimeInputAction(
            key=key,
            value=value_str,
            args=args[1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(time_input_action=time_input_action)
        self._request_actions.append(action)

    def _handle_time_input_operation(self, operation: ops_proto.TimeInputOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        time_value = datetime.strptime(operation.value, '%H:%M:%S').time() if operation.value else None

        if key not in st.session_state:
            st.session_state[key] = time_value

        value = st.session_state[key]

        self._log.debug(f"TimeInput '{key}' value: {value}")

        context = self._get_context(widget_properties.container)

        selected_time = context.call_func(
            "time_input",
            label=widget_properties.label,
            value=value,
            key=key,
            help=widget_properties.help,
            on_change=self._time_input_callback,
            args=[key],
            kwargs={},
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            step=operation.step_seconds,
        )

        self._log.debug(f"TimeInput '{widget_properties.key}' selected: {selected_time}")

    # ------------------------------------------------
    # NumberInput
    # ------------------------------------------------

    def _number_input_callback(self, *args, **kwargs):
        key = args[0]
        value = st.session_state[key]
        self._log.debug(f"NumberInput '{key}' changed to {value}")

        if isinstance(value, int):
            value = commons_proto.IntOrFloat(i=value)
        elif isinstance(value, float):
            value = commons_proto.IntOrFloat(f=value)

        number_input_action = actions_proto.NumberInputAction(
            key=key,
            value=value,
            args=args[1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(number_input_action=number_input_action)
        self._request_actions.append(action)

    def _handle_number_input_operation(self, operation: ops_proto.NumberInputOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        value = operation.value.i if operation.value.HasField('i') else operation.value.f if operation.value.HasField(
            'f') else None

        if key not in st.session_state:
            st.session_state[key] = value
        else:
            value = st.session_state[key]

        self._log.debug(f"NumberInput '{key}' value: {value}")

        context = self._get_context(widget_properties.container)

        min_value = operation.min.i if operation.min.HasField('i') else operation.min.f if operation.min.HasField(
            'f') else None
        max_value = operation.max.i if operation.max.HasField('i') else operation.max.f if operation.max.HasField(
            'f') else None
        step_value = operation.step.i if operation.step.HasField('i') else operation.step.f if operation.step.HasField(
            'f') else None

        selected_number = context.call_func(
            "number_input",
            label=widget_properties.label,
            value=value,
            key=key,
            help=widget_properties.help,
            on_change=self._number_input_callback,
            args=[key],
            kwargs={},
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            min_value=min_value,
            max_value=max_value,
            step=step_value,
        )

        self._log.debug(f"NumberInput '{widget_properties.key}' selected: {selected_number}")

    # ------------------------------------------------
    # TextInput
    # ------------------------------------------------

    def _text_input_callback(self, *args, **kwargs):
        key = args[0]
        value = st.session_state[key]
        self._log.debug(f"TextInput '{key}' changed to {value}")

        text_input_action = actions_proto.TextInputAction(
            key=key,
            value=value,
            args=args[1:],
            kwargs=kwargs
        )

        action = actions_proto.Action(text_input_action=text_input_action)
        self._request_actions.append(action)

    def _handle_text_input_operation(self, operation: ops_proto.TextInputOp):
        widget_properties = operation.widget_props
        key = widget_properties.key
        value = operation.value

        if key not in st.session_state:
            st.session_state[key] = value
        else:
            value = st.session_state[key]

        self._log.debug(f"TextInput '{key}' value: {value}")

        context = self._get_context(widget_properties.container)

        text = context.call_func(
            "text_input",
            label=widget_properties.label,
            value=value,
            key=key,
            help=widget_properties.help,
            on_change=self._text_input_callback,
            args=[key],
            kwargs={},
            disabled=widget_properties.disabled,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
            type='password' if operation.type == ops_proto.TextInputType.PASSWORD else 'default',
            autocomplete=operation.autocomplete,
            placeholder=operation.placeholder,
            max_chars=operation.max_chars
        )

        self._log.debug(f"TextInput '{widget_properties.key}': {text}")

    # ------------------------------------------------
    # Slider
    # ------------------------------------------------

    def _slider_callback(self, *args, **kwargs):
        key = args[0]
        value = st.session_state[key]
        self._log.debug(f"Slider '{key}' changed to {value}")

        if isinstance(value, tuple):
            # Handling range values
            value_range = commons_proto.ValueRange(
                from_value_float=value[0] if isinstance(value[0], float) else None,
                from_value_int=value[0] if isinstance(value[0], int) else None,
                from_value_datetime=value[0].isoformat() if isinstance(value[0], datetime) else None,
                to_value_float=value[1] if isinstance(value[1], float) else None,
                to_value_int=value[1] if isinstance(value[1], int) else None,
                to_value_datetime=value[1].isoformat() if isinstance(value[1], datetime) else None,
            )
            slider_action = actions_proto.SliderAction(key=key, value_range=value_range, args=args[1:], kwargs=kwargs)
        else:
            # Handling single values
            value_single = commons_proto.ValueSingle(
                value_float=value if isinstance(value, float) else None,
                value_int=value if isinstance(value, int) else None,
                value_datetime=value.isoformat() if isinstance(value, datetime) else None
            )
            slider_action = actions_proto.SliderAction(key=key, value_single=value_single, args=args[1:], kwargs=kwargs)

        action = actions_proto.Action(slider_action=slider_action)
        self._request_actions.append(action)

    def _handle_slider_operation(self, operation: ops_proto.SliderOp):
        widget_properties = operation.widget_props
        key = widget_properties.key

        if operation.HasField("value_single"):
            value = (
                operation.value_single.value_float
                if operation.value_single.HasField("value_float")
                else operation.value_single.value_int
                if operation.value_single.HasField("value_int")
                else datetime.fromisoformat(operation.value_single.value_datetime)
            )
        else:  # handle value range
            value = (
                (
                    operation.value_range.from_value_float,
                    operation.value_range.to_value_float,
                )
                if operation.value_range.HasField("from_value_float")

                else (
                    operation.value_range.from_value_int,
                    operation.value_range.to_value_int,
                )
                if operation.value_range.HasField("from_value_int")

                else (
                    datetime.fromisoformat(operation.value_range.from_value_datetime),
                    datetime.fromisoformat(operation.value_range.to_value_datetime),
                )
            )

        if key not in st.session_state:
            st.session_state[key] = value
        else:
            value = st.session_state[key]

        self._log.debug(f"Slider '{key}' value: {value}")

        context = self._get_context(widget_properties.container)

        min_value = (
            operation.min_value_float
            if operation.HasField("min_value_float")
            else operation.min_value_int
            if operation.HasField("min_value_int")
            else datetime.fromisoformat(operation.min_value_datetime)
        )

        max_value = (
            operation.max_value_float
            if operation.HasField("max_value_float")
            else operation.max_value_int
            if operation.HasField("max_value_int")
            else datetime.fromisoformat(operation.max_value_datetime)
        )

        step = (
            operation.step_float
            if operation.HasField("step_float")
            else operation.step_int
            if operation.HasField("step_int")
            else timedelta(seconds=operation.step_time_seconds)
        )

        slider_value = context.call_func(
            "slider",
            label=widget_properties.label,
            value=value,
            key=key,
            help=widget_properties.help,
            on_change=self._slider_callback,
            args=[key],
            kwargs={},
            disabled=widget_properties.disabled,
            min_value=min_value,
            max_value=max_value,
            step=step,
            format=operation.format,
            label_visibility='hidden' if widget_properties.label_visibility == 1 else (
                'collapsed' if widget_properties.label_visibility == 2 else 'visible'),
        )

        self._log.debug(f"Slider '{widget_properties.key}' selected: {slider_value}")

    # ------------------------------------------------
    # PageLink
    # ------------------------------------------------
    def _handle_page_link_operation(self, operation: ops_proto.PageLinkOp):
        widget_properties = operation.widget_props
        context = self._get_context(widget_properties.container)

        if operation.page.startswith(('http://', 'https://')):
            page = operation.page
        else:
            page = st.Page(self._get_or_create_dynamic_get_page_function(operation.page))

        context.call_func(
            "page_link",
            label=widget_properties.label,
            help=widget_properties.help,
            disabled=widget_properties.disabled,
            use_container_width=widget_properties.use_container_width,
            icon=operation.icon if operation.icon else None,
            page=page
        )

    # ------------------------------------------------
    # Rerun
    # ------------------------------------------------
    def _handle_rerun_operation(self, operation: ops_proto.RerunOp):
        st.rerun()

    # ------------------------------------------------
    # SwitchPage
    # ------------------------------------------------
    def _handle_switch_page_operation(self, operation: ops_proto.SwitchPageOp):
        self._log.debug(f"Switching page to: {operation.page}")
        page = st.Page(self._get_or_create_dynamic_get_page_function(operation.page))
        st.switch_page(page)

    # ------------------------------------------------
    # Stop
    # ------------------------------------------------
    def _handle_stop_operation(self, operation: ops_proto.StopOp):
        if operation.message:
            st.warning(operation.message)
        st.stop()

    # ------------------------------------------------
    # End (of operations, possibly terminate)
    # ------------------------------------------------
    def _handle_end_operation(self, operation: ops_proto.EndOp):
        self._log.debug("Received END op")
        if operation.terminate_session:
            self._terminate_session()
            self._post_op_cleanup()
            st.rerun()

    # ------------------------------------------------
    # Terminate
    # ------------------------------------------------
    def _handle_terminate_session_operation(self, operation: ops_proto.TerminateSessionOp):
        self._log.info(f"Received TERMINATE SESSION op: {self._session_id}")
        self._terminate_session()

    def _terminate_session(self):
        self._log.info(f"Terminating session: {self._session_id}")
        self._grpc_client.disconnect()
        for key in st.session_state.keys():
            del st.session_state[key]

    # ------------------------------------------------
    # InnerContainer
    # ------------------------------------------------
    def _handle_inner_container_operation(self, operation: ops_proto.InnerContainerOp):
        # validate_message(operation, 'key')
        height = operation.height if operation.height > 0 else None
        context = self._get_context(operation.parent)
        container = context.container(height=height, border=operation.border)
        self._save_context(operation.key, container)
        self._log.debug(f"Created inner container '{operation.key}' in '{operation.parent}'")

    # ------------------------------------------------
    # ExpandableContainer
    # ------------------------------------------------
    def _handle_expandable_container_operation(self, operation: ops_proto.ExpandableContainerOp):
        # validate_message(operation, 'key')
        context = self._get_context(operation.parent)
        container = context.expander(
            label=operation.label,
            expanded=operation.expanded,
            icon=operation.icon if operation.icon else None
        )
        self._save_context(operation.key, container)
        self._log.debug(f"Created expandable container '{operation.key}' in '{operation.parent}'")

    # ------------------------------------------------
    # PlaceholderContainer (empty)
    # ------------------------------------------------
    def _handle_placeholder_container_operation(self, operation: ops_proto.PlaceholderContainerOp):
        # validate_message(operation, 'key')
        if operation.parent == "" and not operation.empty:
            context = self._get_context(operation.parent)
            container = context.empty()
            self._save_context(operation.key, container)
            self._log.debug(f"Created placeholder (empty) container '{operation.key}' in '{operation.parent}'")
        else:
            context = self._get_context(operation.key)
            if not context:
                self._log.debug(f"Placeholder container '{operation.key}' not found")
            else:
                context.empty()
                self._log.debug(f"Emptied placeholder container '{operation.key}'")

    # ------------------------------------------------
    # Tab Containers
    # ------------------------------------------------
    def _handle_tab_containers_operation(self, operation: ops_proto.TabContainersOp):
        # validate_message(operation, 'tabs')
        context = self._get_context(operation.parent)
        containers = context.tabs(operation.tabs)
        for i, tab in enumerate(operation.tabs):
            self._save_context(operation.keys[i], containers[i])
            self._log.debug(f"Created tab '{tab}' with key {operation.keys[i]} in '{operation.parent}'")

    # ------------------------------------------------
    # Column Containers
    # ------------------------------------------------
    def _handle_column_containers_operation(self, operation: ops_proto.ColumnContainersOp):
        context = self._get_context(operation.parent)
        containers = context.columns(
            operation.widths,

            # Convert ColumnGap enum to string
            gap=ColumnGap(operation.gap).name.lower(),

            # Convert ColumnVerticalAlignment enum to string
            vertical_alignment=ColumnVerticalAlignment(operation.vertical_alignment).name.lower()
        )

        for i, key in enumerate(operation.keys):
            self._save_context(key, containers[i])
            self._log.debug(f"Created column '{key}' in '{operation.parent}'")
