class ActivitiesHelper:
    '''
    Given a pressure data timeseries, calculate a step count
    '''
    def calculate_step_count(self, dataframe, low_threshold = 0, high_threshold = 4):
        p_avg = dataframe.mean(axis=1)
        p_diff = p_avg.diff()

        status = 0
        step_count = 0
        for p_diff_t in p_diff:
            if p_diff_t < low_threshold:
                if status == 1:
                    step_count += 1
                status = -1
            elif p_diff_t > high_threshold:
                status = 1
        return step_count
    
    '''
    Given a pressure timeseries, calculate a cadence
    '''
    def calculate_cadence(self, dataframe):
        duration = (dataframe.index.max() - dataframe.index.min()).seconds
        return self.calculate_step_count(dataframe) / duration
