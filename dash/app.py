from collections import OrderedDict
import dash
import dash_core_components as dcc
import dash_html_components as html
from dash.dependencies import Input, Output
import pandas as pd
import numpy as np
import plotly.graph_objs as go
import plotly.express as px

external_stylesheets = [
    "/assets/style.css"
]

app = dash.Dash(
    external_stylesheets=external_stylesheets,
    meta_tags=[
        {"name": "viewport", "content": "width=device-width, initial-scale=1.0"}
    ],
)

pressure_data = pd.read_csv("../data/sample.csv", index_col=0, parse_dates=[0])

pressure_plots = []
for sensor in [1, 3, 4]:
    series = pressure_data[pressure_data.sensor == sensor]
    scatter = go.Scatter(x = series.index,
                        y = series.pressure,
                        name = f"Sensor {sensor}",
                        opacity = 0.4)
    pressure_plots.append(scatter)    

fig = go.Figure(
    data = pressure_plots,
    layout = go.Layout(
        title = "Pressure timeseries",
        template = "plotly_dark"
    )
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
                dcc.Graph(
                    id="pressure-time-series",
                    figure=fig
                )                
            ],
            className="row",
        ),
    ]
)


if __name__ == "__main__":
    app.run_server(debug=True)
