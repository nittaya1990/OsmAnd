package net.osmand.plus.osmedit.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import net.osmand.AndroidUtils;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.UiUtilities;
import net.osmand.plus.osmedit.EditPoiDialogFragment;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public class SaveExtraValidationDialogFragment extends DialogFragment {

	private static final String TAG = SaveExtraValidationDialogFragment.class.getSimpleName();

	private static final String KEY_MESSAGE_ID = "message_key";

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Context themedContext = UiUtilities.getThemedContext(getActivity(), isNightMode());
		AlertDialog.Builder builder = new AlertDialog.Builder(themedContext);
		builder.setTitle(getString(R.string.are_you_sure))
				.setMessage(getMessageToShow())
				.setPositiveButton(R.string.shared_string_ok, (dialog, which) -> {
					Fragment parent = getParentFragment();
					if (parent instanceof EditPoiDialogFragment) {
						((EditPoiDialogFragment) getParentFragment()).save();
					}
				})
				.setNegativeButton(R.string.shared_string_cancel, null);
		return builder.create();
	}

	private boolean isNightMode() {
		OsmandApplication app = ((OsmandApplication) requireActivity().getApplication());
		return !app.getSettings().isLightContent();
	}

	private String getMessageToShow() {
		String message = getString(R.string.save_poi_without_poi_type_message);
		Bundle args = getArguments();
		if (args != null) {
			int messageId = args.getInt(KEY_MESSAGE_ID, 0);
			if (messageId != 0) {
				message = getString(messageId);
			}
		}
		return message;
	}

	public static void showInstance(@NonNull FragmentManager childFragmentManager, int messageId) {
		if (AndroidUtils.isFragmentCanBeAdded(childFragmentManager, TAG)) {
			SaveExtraValidationDialogFragment fragment = new SaveExtraValidationDialogFragment();
			Bundle args = new Bundle();
			args.putInt(KEY_MESSAGE_ID, messageId);
			fragment.setArguments(args);
			fragment.show(childFragmentManager, TAG);
		}
	}
}