package ru.korniltsev.telegram.profile;

import android.os.Bundle;
import android.support.annotation.Nullable;
import mortar.ViewPresenter;
import ru.korniltsev.telegram.attach_panel.ListChoicePopup;
import ru.korniltsev.telegram.core.flow.pathview.HandlesBack;
import ru.korniltsev.telegram.core.mortar.ActivityOwner;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ProfilePresenter extends ViewPresenter<ProfileView> implements ProfileAdapter.CallBack {
    final ProfilePath path;
    final ActivityOwner owner;
    @Nullable private ListChoicePopup popup;

    @Inject
    public ProfilePresenter(ProfilePath path, ActivityOwner owner) {
        this.path = path;
        this.owner = owner;
    }

    @Override
    protected void onLoad(Bundle savedInstanceState) {
        super.onLoad(savedInstanceState);
        getView().bindUser(path.user);

    }

    @Override
    public void clicked(ProfileAdapter.Item item) {
        if (item.bottomSheetActions != null) {
            popup = ListChoicePopup.create(owner.expose(), item.bottomSheetActions);
        }
    }



    public boolean hidePopup() {
        if (popup != null){
            popup.dismiss();
            return true;
        }
        return false;
    }
}
