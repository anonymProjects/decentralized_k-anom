# Decentralized k-Anonymization of Trajectories via Tit-for-Tat.

This repository contains the code used in the experiments for the anonymization of a data set of trajectories applying the following methods:
  * Centralized microaggregation
  * Decentralized microagregation sending:
    * Trajectory
    * Centroid

The metrics calculated are:
  * Mean RMSE (utility of the anonymized data set)
  * Mean number of messages (network overhead)


## Installing
The computer should fulfill the following requirements:
* Java (RE or DK) environment v8 must be installed (or, alternatively, OpenJDK 8). Java 8 can be downloaded from: https://www.java.com/en/download/
* At least 4 GB of RAM are recommended.

## Code
The source code is available in this repository inside the ['src'](https://github.com/anonymProjects/decentralized_k-anom/tree/master/src/anomTrajectories) folder.

The source code can be imported to Java IDEs (e.g. Eclipse) by cloning or downloading the project from the [main page](https://github.com/anonymProjects/decentralized_k-anom) on github.

## Running
To run, access the folder ['src'](https://github.com/anonymProjects/decentralized_k-anom/tree/master/src/anomTrajectories) and execute the following tests to obtain the described results:
```
Main1.java
```
Executes the centralized microaggregation of the cabs dataset, obtaining the resulting RMSE for k values from 2 to 100 (Fig. 4):

| k   | Mean RMSE |
| --- | --------- |
| 2   | 378.502   |
| 5   | 554.752   |
| 10  | 659.507   |
| 15  | 718.276   |
| 20  | 762.185   |
| 25  | 794.115   |
| 30  | 820.920   |
| 35  | 845.467   |
| 40  | 868.351   |
| 45  | 887.904   |
| 50  | 907.605   |
| 55  | 928.798   |
| 60  | 947.203   |
| 65  | 960.638   |
| 70  | 977.253   |
| 75  | 985.628   |
| 80  | 994.222   |
| 85  | 1012.450  |
| 90  | 1024.536  |
| 95  | 1033.467  |
| 100 | 1040.726  |

```
Main2.java
```
Executes the decentralized microaggregation (trajectory) of the cabs dataset, obtaining the resulting RMSE and the mean number of messages per peer for k values from 2 to 100 (Fig. 4 and Fig. 5):


| k   | Mean RMSE | Mean # messages per peer |
| --- | --------- | ------------------------ |
| 2   | 829.011   | 2.997                    |
| 5   | 1249.403  | 4.844                    |
| 10  | 1517.170  | 5.577                    |
| 15  | 1670.018  | 6.078                    |
| 20  | 1790.866  | 6.933                    |
| 25  | 1888.999  | 7.873                    |
| 30  | 1978.592  | 8.387                    |
| 35  | 2037.743  | 7.984                    |
| 40  | 2090.358  | 10.816                   |
| 45  | 2147.086  | 11.608                   |
| 50  | 2215.658  | 13.439                   |
| 55  | 2253.932  | 14.753                   |
| 60  | 2301.758  | 17.588                   |
| 65  | 2352.015  | 20.827                   |
| 70  | 2384.932  | 24.090                   |
| 75  | 2440.204  | 26.567                   |
| 80  | 2463.530  | 27.819                   |
| 85  | 2488.998  | 33.101                   |
| 90  | 2530.928  | 34.594                   |
| 95  | 2563.044  | 36.690                   |
| 100 | 2607.443  | 39.814                   |

```
Main3.java
```
Executes the decentralized microaggregation (centroid) of the cabs dataset, obtaining the resulting RMSE and the mean number of messages per peer for k values from 2 to 100 (Fig. 4 and Fig. 5):


| k   | Mean RMSE | Mean # messages per peer |
| --- | --------- | ------------------------ |
| 2   | 1117.484  | 11.523                   |
| 5   | 1427.489  | 15.271                   |
| 10  | 1663.444  | 15.161                   |
| 15  | 1818.147  | 14.762                   |
| 20  | 1932.335  | 15.386                   |
| 25  | 2025.659  | 15.945                   |
| 30  | 2112.497  | 15.791                   |
| 35  | 2154.031  | 15.288                   |
| 40  | 2217.400  | 18.178                   |
| 45  | 2264.410  | 19.339                   |
| 50  | 2327.775  | 21.141                   |
| 55  | 2376.741  | 23.038                   |
| 60  | 2425.234  | 25.284                   |
| 65  | 2447.926  | 28.777                   |
| 70  | 2508.355  | 32.536                   |
| 75  | 2546.443  | 35.395                   |
| 80  | 2579.363  | 38.386                   |
| 85  | 2590.722  | 41.734                   |
| 90  | 2617.526  | 43.599                   |
| 95  | 2658.882  | 45.743                   |
| 100 | 2695.587  | 51.680                   |

```
Main4.java
```
Executes the decentralized microaggregation (trajectory) of the cabs dataset, obtaining the resulting RMSE and the mean number of messages per peer, varying the % of bad peers in the system in the range from 0.0% to 10.0% (Fig. 6). The RMSE is calculated distinguishing between good and bad peers (Fig. 7):


| % Bad peers | Mean # messages per peer | Mean RMSE good peers | Mean RMSE bad peers |
| ----------- | ------------------------ | ---------------------| --------------------|
| 0.0         | 5.565                    | 1519.945             | N/A                 |
| 1.0         | 79.443                   | 1520.709             | 1668.581            |
| 2.0         | 153.343                  | 1520.195             | 1677.454            |
| 3.0         | 212.513                  | 1515.542             | 1679.122            |
| 4.0         | 298.921                  | 1516.041             | 1682.642            |
| 5.0         | 341.475                  | 1512.926             | 1687.919            |
| 6.0         | 442.360                  | 1512.218             | 1692.205            |
| 7.0         | 517.867                  | 1513.316             | 1685.431            |
| 8.0         | 571.350                  | 1508.649             | 1681.033            |
| 9.0         | 681.607                  | 1506.259             | 1692.745            |
| 10.0        | 750.995                  | 1506.365             | 1689.458            |

```
Main5.java
```
Executes the decentralized microaggregation (centroid) of the cabs dataset, obtaining the mean number of messages per peer, varying the % of bad peers in the system in the range from 0.0% to 10.0% (Fig. 6):


| % Bad peers | Mean # messages per peer |
| ----------- | ------------------------ |
| 0.0         | 15.108                   |
| 1.0         | 98.528                   |
| 2.0         | 163.111                  |
| 3.0         | 237.152                  |
| 4.0         | 310.758                  |
| 5.0         | 387.424                  |
| 6.0         | 452.489                  |
| 7.0         | 530.937                  |
| 8.0         | 617.439                  |
| 9.0         | 714.774                  |
| 10.0        | 763.091                  |
