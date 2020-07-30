import dash
import dash_core_components as dcc
import dash_html_components as html
import pandas as pd
import plotly.graph_objs as go
import plotly.express as px

app = dash.Dash()

pressure_data = pd.read_csv("../data/sample.csv", index_col=0, parse_dates=[0])
#pressure_ts = px.line(pressure_data, se='Date', y='AAPL.High')

#pressure_data_by_sensor = ...

s = go.Scatter(x=pressure_data.index,
               y=pressure_data.pressure,
               name = "Pressure timeseries",
               line = dict(color = 'blue'),
               opacity = 0.4)

layout = dict(title='Pressure timeseries')

fig = dict(data=[s], layout=layout)


app.layout = html.Div([
    dcc.Graph(
        id='pressure-time-series',
        figure=fig
    )
])

if __name__ == '__main__':
    app.run_server(debug=True)
