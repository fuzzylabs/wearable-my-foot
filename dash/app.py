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
from graphs import *
import os

activities = ActivitiesHelper()
graphs = GraphsHelper()

# Data + graph initialisation
pressure_data = pd.read_csv("../data/sample.csv", index_col=0, parse_dates=[0])
pressure_figure = graphs.generate_timeseries_plot(pressure_data)

# accelerometer_data = pd.read_csv("../data/accelerometer.csv")
# accelerometer_figure = graphs.generate_accelerometer_plot(accelerometer_data)

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
    "/assets/style_extra.css", "/assets/style.css"
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
                    className="11 columns",
                ),
                html.Div(
                    [
                        html.Img(
                            src="/assets/logo.png"
                        )
                    ],
                    className="1 column"
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
                )
            ],
            className="row",
            style={
                "background-color": "#111"
            }
        ),
        # html.Div(
        #     [
        #         dcc.Graph(
        #             id="accelerometer-time-series",
        #             figure=accelerometer_figure
        #         )
        #     ],
        #     className="row"
        # ),
    ]
)

if __name__ == "__main__":
    app.run_server(debug=True)
