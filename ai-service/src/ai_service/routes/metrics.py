from prometheus_flask_exporter import PrometheusMetrics

metrics = None

def init_metrics(app):
    global metrics
    metrics = PrometheusMetrics(app, path="/metrics")
    metrics.info("app_info", "AI Service", version="1.0.0")
