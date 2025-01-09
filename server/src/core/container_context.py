class ContainerContext:
    def __init__(self, context):
        self.context = context

    def call_func(self, func_name, *args, **kwargs):
        """
        Calls a function of the Streamlit context with the given function name and arguments.
        """
        func = getattr(self.context, func_name)

        if func is None or not callable(func):
            raise AttributeError(f"{func_name} is not a method of this object")

        return func(*args, **kwargs)

    def container(self, *args, **kwargs):
        return ContainerContext(self.context.container(*args, **kwargs))

    def expander(self, *args, **kwargs):
        return ContainerContext(self.context.expander(*args, **kwargs))

    def tabs(self, *args, **kwargs):
        containers = self.context.tabs(*args, **kwargs)
        return [ContainerContext(container) for container in containers]

    def columns(self, *args, **kwargs):
        containers = self.context.columns(*args, **kwargs)
        return [ContainerContext(container) for container in containers]

    def empty(self):
        return ContainerContext(self.context.empty())
