package ev_route_planner.model.geolocation;

public class WifiAccessPoints {

    String macAddress;
    int signalStrength;
    int age;
    int channel;
    int signalToNoiseRatio;

    // Age and signal to noise ratio are not measured and are thus set to 0.
    public WifiAccessPoints() {
        age = 0;
        signalToNoiseRatio = 0;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public int getSignalStrength() {
        return signalStrength;
    }

    public void setSignalStrength(int signalStrength) {
        this.signalStrength = signalStrength;
    }

    public int getChannel() {
        return channel;
    }

    public void setChannel(int channel) {
        this.channel = channel;
    }
}
