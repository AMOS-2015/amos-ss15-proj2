package org.croudtrip.fragments;


import android.os.Bundle;
import android.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.croudtrip.R;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_maps, container, false);

        MapFragment map = (MapFragment) getFragmentManager().findFragmentById(R.id.location_map);
        map.getMapAsync( new MapReady() );

        return view;
    }

    class MapReady implements OnMapReadyCallback {

        @Override
        public void onMapReady(GoogleMap map) {
            map.setMyLocationEnabled(true);

            // do some testing movement on the map
            LatLng place = new LatLng(45.5017123, -73.5652739);
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(place, 13));

            map.addMarker(new MarkerOptions()
                    .title("Sydney")
                    .snippet("The most populous city in Australia.")
                    .position(place));

            // TODO: call the server and do a directions request and read the polyline out of it and draw it on the map

            // try to decode a polyline
            map.addPolyline( new PolylineOptions().addAll(PolyUtil.decode("e`miGhmocNyOcAuAoKoD_k@oKqi@cNydAc^`E}~@na@{{@{E{U{Dce@mg@iEicAq_@w{@wd@wNsUi[iSgAmSfPwh@xLq{BvNepAlPsr@~D_DuGwCe}BqTciB}_AanHy^mzByWsp@_b@elDoRw{CpVqmApA}k@uXeq@qgB{eDc~BcxFot@csB{dBsiMk`@o}CdAiaAbFkwCqoBixMyPqmAm@_mAze@}aBr[c{@lAkl@uw@q}LiO}nAcl@w}Ae`@ucC_Za}HekAaqJ__EwnZmqByaO{RogEePczCmjA}uHrXwdBiKikCwk@cgC{Vsj@aCw~@}Gwr@u\\aw@ad@wlCtQs}DdZsrAgfAqrKkw@iaLy[k`Dya@akDoj@anB}i@sjByQ{fAef@ct@_`@qqAmWi_BiJguAeu@wbAo\\as@wLcsBugAmdFyq@erA{m@slCwdA{qDwIqfAiHeyCau@mfB}eBy|E}hBemFulBmqLojBwcLo~@uiDwwA}rJkGeaAdWmaBzB_dAih@}gHyiCwq^sw@iiJus@sqCwMm`BXwxA{b@iyBmEyaCeSe}EaN{oJc\\_cRcYumSaO_vBzaAqoDly@{oGfCmbB}Qul@or@uqC}}@a|E_d@uaAaj@{k@moAeuDem@wyIeo@qkPgk@ibDgg@seGa@gaAnJmqBam@abEwsAyuE}sA_fFAqbEie@uuDkP}y@_k@oaAyg@mvAqq@guA}w@e`Aeo@atAc|@{z@_qBel@m`CihBykBghBcwBa~Bkd@at@aV}mBpD{eAh@sm@ml@ueBwUcU{oApBog@oNoa@_h@emA{rBahAog@}]i^wu@ipB{aAi}AcwAoxB{iBkjDkwAwjBeoIytKu_AapAkXypAyY_qAsaDupFo`Ao`B_n@ss@owAo}@{~GsiMy}EalJa`DoyHayDsqMonEcrNucBucFuqAwa@gmEo|NywEegPwrAymFmf@s|CbBsaB`aAyhDhO{qAlCggG}EqwAaXubAca@kxAyC{_BgN}cEag@ueCi}BmlHkfFo`N_p@sgByd@eo@wfAwqCitDmsMym@}f@}jAquC{}AsmD_q@o{C_hFo`N}Ykt@u}AwnByaBsxBgx@skBemDobLywBwwGq_BagCa`@ab@ih@ksA_Q_i@}DihBum@gvCs_Aez@wYidAov@awC{p@yfDef@egFodAmuGu[mjAgHwjAxLkmGmHgy@xCeyFdEopCf]e{BwcCyiGy_@{s@{ZiPqq@ej@ciAumBeMa]mJaByFqGwI`UwI{H")) );

        }

        /**
         * Decodes an encoded string of a polyline given by a directions request to a list of points
         * @param encoded The encoded polyline string
         * @return a list of points that are described by the polyline.
         */
        private List<LatLng> decodePolyline( String encoded ) {
            List<LatLng> poly = new ArrayList<LatLng>();
            int index = 0, len = encoded.length();
            int lat = 0, lng = 0;

            while (index < len) {
                int b, shift = 0, result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lat += dlat;

                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                lng += dlng;

                LatLng p = new LatLng((int) (((double) lat / 1E5) * 1E6),
                        (int) (((double) lng / 1E5) * 1E6));
                poly.add(p);
            }

            return poly;
        }
    }
}
