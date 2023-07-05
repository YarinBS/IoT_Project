import numpy as np
def main(h):
    arr = np.array([2, h, 4])
    arr = np.array2string(arr, precision=2, separator=',',
                          suppress_small=True)
    pass
    return arr
