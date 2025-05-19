package hu.szte.nyanusz.mobilalk.model;

public class Music {
    private String id;
    private String cim;
    private String eloado;
    private String mufaj;
    private String albumNev;
    private int duration;
    private String mp3Url;
    private String albumArtUri;
    private boolean likeolt;
    private String feltolto;
    private String firebaseId;

    public Music() {
    }

    public Music(String id, String cim, String eloado, String mufaj, String albumNev, int duration,
                 String mp3Url, String albumArtUri, boolean likeolt, String feltolto) {
        this.id = id;
        this.cim = cim;
        this.eloado = eloado;
        this.mufaj = mufaj;
        this.albumNev = albumNev;
        this.duration = duration;
        this.mp3Url = mp3Url;
        this.albumArtUri = albumArtUri;
        this.likeolt = likeolt;
        this.feltolto = feltolto;
    }

    // Getterek és setterek
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getCim() { return cim; }
    public void setCim(String cim) { this.cim = cim; }
    public String getEloado() { return eloado; }
    public void setEloado(String eloado) { this.eloado = eloado; }
    public String getMufaj() { return mufaj; }
    public void setMufaj(String mufaj) { this.mufaj = mufaj; }
    public String getAlbumNev() { return albumNev; }
    public void setAlbumNev(String albumNev) { this.albumNev = albumNev; }
    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }
    public String getMp3Url() { return mp3Url; }
    public void setMp3Url(String mp3Url) { this.mp3Url = mp3Url; }
    public String getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(String albumArtUri) { this.albumArtUri = albumArtUri; }
    public boolean isLikeolt() { return likeolt; }
    public void setLikeolt(boolean likeolt) { this.likeolt = likeolt; }
    public String getFeltolto() { return feltolto; }
    public void setFeltolto(String feltolto) { this.feltolto = feltolto; }
    public String getFirebaseId() { return firebaseId; }
    public void setFirebaseId(String firebaseId) { this.firebaseId = firebaseId; }
}