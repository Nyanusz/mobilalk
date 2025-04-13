package hu.szte.nyanusz.mobilalk.model;

import android.net.Uri;
import com.google.firebase.firestore.PropertyName;

public class Music {
    private String firebaseId;
    private String mufaj;
    private String cim;
    private String eloado;
    private String feltoltoID;
    private int hossz;
    private Uri albumArtUri;

    // Üres konstruktor a Firestore számára
    public Music() {
    }

    // Getterek és setterek
    public String getFirebaseId() {
        return firebaseId;
    }

    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }

    @PropertyName("mufaj")
    public String getMufaj() {
        return mufaj;
    }

    @PropertyName("mufaj")
    public void setMufaj(String mufaj) {
        this.mufaj = mufaj;
    }

    @PropertyName("cim")
    public String getCim() {
        return cim;
    }

    @PropertyName("cim")
    public void setCim(String cim) {
        this.cim = cim;
    }

    @PropertyName("eloado")
    public String getEloado() {
        return eloado;
    }

    @PropertyName("eloado")
    public void setEloado(String eloado) {
        this.eloado = eloado;
    }

    @PropertyName("feltoltoID")
    public String getFeltoltoID() {
        return feltoltoID;
    }

    @PropertyName("feltoltoID")
    public void setFeltoltoID(String feltoltoID) {
        this.feltoltoID = feltoltoID;
    }

    @PropertyName("hossz")
    public int getHossz() {
        return hossz;
    }

    @PropertyName("hossz")
    public void setHossz(int hossz) {
        this.hossz = hossz;
    }

    public Uri getAlbumArtUri() {
        return albumArtUri;
    }

    public void setAlbumArtUri(Uri albumArtUri) {
        this.albumArtUri = albumArtUri;
    }
}