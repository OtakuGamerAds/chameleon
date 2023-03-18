package mchorse.chameleon.lib;

import mchorse.chameleon.lib.data.animation.Animations;
import mchorse.chameleon.lib.data.model.ModelBone;
import mchorse.chameleon.lib.data.model.Model;
import net.minecraft.client.renderer.GLAllocation;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.google.common.collect.ImmutableList;

@SideOnly(Side.CLIENT)
public class ChameleonModel
{
    public Model model;
    public Animations animations;
    public long lastUpdate;

    private List<String> boneNames;
    private boolean isStatic;
    private List<File> files;
    public String thumbnailFullPath;

    public int displayListId = -1;

    public ChameleonModel(Model model, Animations animations, List<File> files, long lastUpdate) {
        this.model = model;
        this.animations = animations;
        this.files = files;
        this.lastUpdate = lastUpdate;
        this.isStatic = animations == null || animations.getAll().isEmpty();

        this.thumbnailFullPath = getThumbnailFullPath();
    }

    public String getThumbnailFullPath() {
        String fullPath = null;

        for (File file : this.files) {
            if (file.getName().endsWith(".geo.json")) {
                File thumbnailFile = new File(file.getParentFile(), "thumbnail.png");
                if (thumbnailFile.exists()) {
                    fullPath = thumbnailFile.getAbsolutePath();
                    break;
                }
            }
        }

        return fullPath;
    }

    public List<String> getBoneNames()
    {
        if (this.boneNames != null)
        {
            return this.boneNames;
        }

        return this.boneNames = this.getBoneNames(new ArrayList<String>(), this.model.bones);
    }

    public List<String> getChildren(String parent)
    {
        List<String> children = new ArrayList<String>();

        for (ModelBone bone : this.model.bones)
        {
            if (Objects.equals(bone.id, parent))
            {
                this.getBoneNames(children, ImmutableList.of(bone));

                break;
            }
        }

        return children;
    }

    private List<String> getBoneNames(List<String> boneNames, List<ModelBone> bones)
    {
        for (ModelBone bone : bones)
        {
            boneNames.add(bone.id);

            this.getBoneNames(boneNames, bone.children);
        }

        return boneNames;
    }

    public boolean isStatic()
    {
        return this.isStatic;
    }

    public boolean isStillPresent()
    {
        for (File file : this.files)
        {
            if (!file.exists())
            {
                return false;
            }
        }

        return true;
    }

    public List<File> getFiles(){
        return this.files;
    }

    public void cleanup() {
        if (displayListId != -1) {
            GLAllocation.deleteDisplayLists(displayListId);
            displayListId = -1;
        }
    }

}
