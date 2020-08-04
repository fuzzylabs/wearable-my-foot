class ActivitiesHelper:
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
