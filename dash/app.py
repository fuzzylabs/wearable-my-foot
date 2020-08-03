from collections import OrderedDict
import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
import pandas as pd
import numpy as np
import plotly.express as px
from activities import *

activities = ActivitiesHelper()

# Data + graph initialisation
pressure_data = pd.read_csv("../data/sample.csv", index_col=0, parse_dates=[0])
pressure_figure = activities.generate_timeseries_plot(pressure_data)

# Dash app setup
external_stylesheets = [
    "/assets/style.css"
]

app = dash.Dash(
    external_stylesheets=external_stylesheets,
    meta_tags=[
        {"name": "viewport", "content": "width=device-width, initial-scale=1.0"}
    ],
)

def generate_layout(pressure_figure):
    return html.Div(
        [
            html.Div(
                id="header",
                children=[
                    html.Div(
                        [
                            html.H3(
                                "Wearable My Foot visualisation"
                            )
                        ],
                        className="eight columns",
                    )
                ],
                className="row",
            ),
            html.Div(
                [
                    dcc.Graph(
                        id="pressure-time-series",
                        figure=pressure_figure
                    )                
                ],
                className="row",
            ),
        ]
    )

app.layout = generate_layout(pressure_figure)

if __name__ == "__main__":
    app.run_server(debug=True)
