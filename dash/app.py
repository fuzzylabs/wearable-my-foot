from collections import OrderedDict
import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
import pandas as pd
import numpy as np
import plotly.express as px
import dash_daq as daq
from activities import *

activities = ActivitiesHelper()

# Data + graph initialisation
pressure_data = pd.read_csv("../data/sample.csv", index_col=0, parse_dates=[0])
pressure_figure = activities.generate_timeseries_plot(pressure_data)

step_count_figure = daq.LEDDisplay(
    id = "step-count",
    value = activities.calculate_step_count(pressure_data),
    color = "#fff",
    backgroundColor = "#1e2130"
)

data_count_figure = daq.LEDDisplay(
    id = "data-count",
    value = len(pressure_data),
    color = "#fff",
    backgroundColor = "#1e2130"
)

candence_figure = daq.LEDDisplay(
    id = "cadence",
    value = activities.calculate_cadence(pressure_data),
    color = "#fff",
    backgroundColor = "#1e2130"
)

# Dash app setup
external_stylesheets = [
    "/assets/style.css",
    "https://codepen.io/chriddyp/pen/bWLwgP.css"
]

app = dash.Dash(
    external_stylesheets=external_stylesheets,
    meta_tags=[
        {"name": "viewport", "content": "width=device-width, initial-scale=1.0"}
    ],
)

app.layout = html.Div(
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
                html.Center(
                    [
                        html.Div(
                            [
                                html.P("Data points"),
                                html.P(" "),
                                data_count_figure,
                                html.P("Step count"),
                                html.P(" "),
                                step_count_figure,
                                html.P("Cadence"),
                                html.P(" "),
                                candence_figure,

                        ],
                            className="two columns"
                        ),
                    ]
                ),
                html.Div(
                    [
                        dcc.Graph(
                            id="pressure-time-series",
                            figure=pressure_figure
                        )
                    ],
                    className="ten columns",
                ),
            ],
            className="row"
        )
    ]
)

if __name__ == "__main__":
    app.run_server(debug=True)
