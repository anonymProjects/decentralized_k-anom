package anomTrajectories;

public class Result {
	double item;
	double meanRmse;
	double meanRmseBadPeers;
	double meanRmseNormalPeers;
	double SSE;
	double TMD;
	long runTime;
	int numMessages;
	
	public Result(double meanRmse, double SSE) {
		this.meanRmse = meanRmse;
		this.SSE = SSE;
	}
	
	public Result() {
		this.meanRmse = 0;
		this.SSE = 0;
		this.runTime = 0;
		this.numMessages = 0;
		this.meanRmseBadPeers = 0;
		this.meanRmseNormalPeers = 0;
		this.TMD = 0.0;
	}
	
}
