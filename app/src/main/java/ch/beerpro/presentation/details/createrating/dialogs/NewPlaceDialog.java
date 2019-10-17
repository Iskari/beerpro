package ch.beerpro.presentation.details.createrating.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import ch.beerpro.R;

public class NewPlaceDialog extends DialogFragment {
    AlertDialog.Builder builder;

    public interface PlaceDialogListener {
        public void onDialogPositiveClick(String name, String address);
    }

    PlaceDialogListener listener;


    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        builder = new AlertDialog.Builder(getActivity());
        // Get the layout inflater
        LayoutInflater inflater = requireActivity().getLayoutInflater();

        // Inflate and set the layout for the dialog
        // Pass null as the parent view because its going in the dialog layout
        View v = inflater.inflate(R.layout.dialog_new_place, null);
        EditText name = v.findViewById(R.id.placeName);
        EditText address = v.findViewById(R.id.address);

        builder.setView(v)
                // Add action buttons
                .setPositiveButton(R.string.ok, (dialog, id) -> {
                    if(name.getText().length() <= 1 || address.getText().length() <= 1){
                        if(getActivity() != null) {
                            Toast.makeText(getActivity().getApplicationContext(), getText(R.string.noPlaceError), Toast.LENGTH_SHORT).show();
                        }
                    }else {
                        listener.onDialogPositiveClick(name.getText().toString(), address.getText().toString());
                        NewPlaceDialog.this.getDialog().dismiss();
                    }
                })
                .setNegativeButton(R.string.cancel, (dialog, id) -> NewPlaceDialog.this.getDialog().cancel());
        return builder.create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // Verify that the host activity implements the callback interface
        try {
            // Instantiate the NoticeDialogListener so we can send events to the host
            listener = (PlaceDialogListener) context;
        } catch (ClassCastException e) {
            // The activity doesn't implement the interface, throw exception
            throw new ClassCastException("Must implement PlaceDialogListener");
        }
    }

}
