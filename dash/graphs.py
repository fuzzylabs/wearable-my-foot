import plotly.graph_objs as go

class GraphsHelper:
    template = "plotly_dark"

    '''
    Generate a plot for a timeseries
    '''
    def generate_timeseries_plot(self, dataframe):
        pressure_plots = []
        for sensor in ["p1", "p2", "p3"]:
            series = dataframe[sensor]
            scatter = go.Scatter(x = dataframe.index,
                                 y = series,
                                 name = f"Sensor {sensor}",
                                 opacity = 0.4)
            pressure_plots.append(scatter)

        pressure_figure = go.Figure(
            data = pressure_plots,
            layout = go.Layout(
                title = "Pressure timeseries",
                template = self.template
            )
        )
        return pressure_figure
