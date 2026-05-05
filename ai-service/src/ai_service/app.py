from flask import Flask
from ai_service.routes.health import bp as health_bp
from ai_service.routes.webhook import bp as webhook_bp
from ai_service.routes.metrics import init_metrics

def create_app(testing: bool = False) -> Flask:
    app = Flask(__name__)
    app.config["TESTING"] = testing
    app.register_blueprint(health_bp)
    app.register_blueprint(webhook_bp)
    if not testing:
        init_metrics(app)
    return app
