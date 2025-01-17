package net.osmand.plus.osmedit;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import net.osmand.osm.edit.Entity;
import net.osmand.osm.edit.EntityInfo;
import net.osmand.plus.views.layers.ContextMenuLayer.ApplyMovedObjectCallback;

class SaveOsmChangeAsyncTask extends AsyncTask<Void, Void, Entity> {

	private final OpenstreetmapLocalUtil mOpenstreetmapUtil;
	@Nullable
	private final ApplyMovedObjectCallback mCallback;
	private final OpenstreetmapPoint objectInMotion;

	SaveOsmChangeAsyncTask(@NonNull OpenstreetmapLocalUtil openstreetmapUtil,
						   @NonNull OpenstreetmapPoint objectInMotion,
						   @Nullable ApplyMovedObjectCallback callback) {
		this.mOpenstreetmapUtil = openstreetmapUtil;
		this.mCallback = callback;
		this.objectInMotion = objectInMotion;
	}

	@Override
	protected Entity doInBackground(Void... params) {
		Entity entity = objectInMotion.getEntity();
		EntityInfo entityInfo = mOpenstreetmapUtil.getEntityInfo(entity.getId());
		return mOpenstreetmapUtil.commitEntityImpl(objectInMotion.getAction(), entity,
				entityInfo, "", false, entity.getChangedTags());
	}

	@Override
	protected void onPostExecute(Entity newEntity) {
		if (mCallback != null) {
			mCallback.onApplyMovedObject(newEntity != null, objectInMotion);
		}
	}
}
