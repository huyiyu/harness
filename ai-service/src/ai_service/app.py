from flask import Flask
from ai_service.routes.health import bp as health_bp

def create_app(testing: bool = False) -> Flask:
    app = Flask(__name__)
    app.config["TESTING"] = testing
    app.register_blueprint(health_bp)
    return app
