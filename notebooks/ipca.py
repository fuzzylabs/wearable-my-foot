from dataclasses import dataclass
import numpy as np

@dataclass
class IncrementalPCA:
    B: int = None
    A: np.ndarray = None
    mean: np.ndarray = None
    eigenvalues: np.ndarray = None
    eigenvectors: np.ndarray = None
    
    def __init__(self, initial_sample_size):
        self.B = initial_sample_size
        
    def _update_mean(self, x, n):
        self.mean = self.mean + 1 / (n + 1) * (x - self.mean)
    
    def _update_eigen(self):
        vals, vecs = np.linalg.eig(self.A)
        self.eigenvectors = vecs[:,np.argsort(vals)[::-1]]
        self.eigenvalues = np.sort(vals)[::-1]
        
    def _get_transformed(self, x):
        return np.dot(x, self.eigenvectors)
        
    def fit_transform(self, X):
        X_initial = X[:self.B]
        self.mean = np.mean(X_initial, axis=0)
        X_centered = X_initial - self.mean
        
        self.A = np.cov(X_centered.T)
        self._update_eigen()
        X_out = self._get_transformed(X_centered)
        
        for n in range(self.B, len(X)):
            x = np.reshape(X[n], (1,-1))
            self._update_mean(x, n)
            x_centered = x - self.mean
            self.A = (n - 1) / n * self.A + np.dot(x_centered.T, x_centered) / (n ** 2)
            
            self._update_eigen()
            X_out = np.vstack([X_out, self._get_transformed(x_centered)])
            
        return X_out