import plotly.graph_objs as go

class ActivitiesHelper:
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
        trace1 = {
            "name": "x-component", 
            "type": "scatter",
            "x": dataframe["x_component_x"],
            "y": dataframe["x_component_y"]
        }
        trace2 = {
            "name": "y-component",
            "type": "scatter",
            "x": dataframe["y_component_x"],
            "y": dataframe["y_component_y"]
        }
        
        data = [trace1, trace2]
        return go.Figure(
            data=data,
            layout = go.Layout(
                title = "Accelerometer",
                template = self.template
            )
        )

    '''
    Given a pressure data timeseries, calculate a step count
    '''
    def calculate_step_count(self, pressure_data):
        return len(pressure_data[pressure_data.sensor == 1])
    
    '''
    Given a pressure timeseries, calculate a cadence
    '''
    def calculate_cadence(self, pressure_data):
        s1 = pressure_data[pressure_data.sensor == 1].pressure
        return s1.resample("s").mean().mean()
