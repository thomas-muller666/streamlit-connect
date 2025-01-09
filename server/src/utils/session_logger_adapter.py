import logging


class SessionLoggerAdapter(logging.LoggerAdapter):

    def process(self, msg, kwargs):
        return '[%s] %s' % (self.extra['session_id'], msg), kwargs
