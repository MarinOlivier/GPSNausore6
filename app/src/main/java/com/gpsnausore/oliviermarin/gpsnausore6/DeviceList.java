package com.gpsnausore.oliviermarin.gpsnausore6;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link DeviceList.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link DeviceList#newInstance} factory method to
 * create an instance of this fragment.
 */
public class DeviceList extends Fragment implements AdapterView.OnItemClickListener {

    private OnFragmentInteractionListener mListener;
    private Set<BluetoothDevice> pairedDevices;
    private BluetoothAdapter myBluetooth;
    private ListView listView;
    private View myView;
    private ArrayList list = new ArrayList();
    private Button disconnectDeviceButton;
    private TextView connectedDevice;

    public DeviceList() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeviceList.
     */
    // TODO: Rename and change types and number of parameters
    public static DeviceList newInstance() {
        DeviceList fragment = new DeviceList();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {

        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_device_list, container, false);

        disconnectDeviceButton = view.findViewById(R.id.disconnectDeviceButton);
        ((MainActivity)getActivity()).setConnectedDeviceText((TextView) view.findViewById(R.id.connectedDeviceText));
        ((MainActivity)getActivity()).setDisconnectDeviceButton((Button) view.findViewById(R.id.disconnectDeviceButton));
        disconnectDeviceButton.setEnabled(false);

        if (((MainActivity)getActivity()).isConnected())
            ((MainActivity)getActivity()).setConnectedDeviceView();


        connectedDevice = view.findViewById(R.id.connectedDeviceText);

        listView = (ListView)view.findViewById(R.id.devicesList);
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, list);
        listView.setOnItemClickListener(this);

        listView.setAdapter(arrayAdapter);

        this.myView = view;
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
        checkBluetooth();
        pairedDevicesList();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    private void checkBluetooth(){
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(myBluetooth == null) {
            //Show a mensag. that thedevice has no bluetooth adapter
            Toast.makeText(getActivity().getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            getActivity().finish();
        }
        else {
            if (!myBluetooth.isEnabled()) {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }
        }
    }

    private void pairedDevicesList() {
        pairedDevices = myBluetooth.getBondedDevices();
        if (pairedDevices.size()>0) {
            for(BluetoothDevice bt : pairedDevices) {
                list.add(bt.getName() + "\n" + bt.getAddress()); //Get the device's name and the address
            }
        }
        else {
            Toast.makeText(getActivity().getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View v, int arg2, long arg3) {
        String val = ((TextView) v).getText().toString().trim();
        Log.w("DEBUG", ((TextView) v).getText().toString().split("\\n")[0]);
        Log.w("DEBUG", ((TextView) v).getText().toString().split("\\n")[1]);

        ((MainActivity)getActivity()).startConnection(((TextView) v).getText().toString().split("\\n")[0],
                ((TextView) v).getText().toString().split("\\n")[1],
                myBluetooth);
    }

    public void setConnectedDeviceText(String connectedDeviceText) {
        this.connectedDevice.setText(connectedDeviceText);
    }
}
