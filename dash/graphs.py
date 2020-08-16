import plotly.graph_objs as go

class GraphsHelper:
    template = "plotly_dark"

    '''
    Generate a plot for a timeseries
    '''
    def generate_timeseries_plot(self, dataframe):
        pressure_plots = []
        for sensor in [1, 3, 4]:
            series = dataframe[dataframe.sensor == sensor]
            scatter = go.Scatter(x = series.index,
                                y = series.pressure,
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

    def generate_accelerometer_plot(self, dataframe):
        pass
