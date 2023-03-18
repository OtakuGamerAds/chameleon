package mchorse.chameleon.animation;

import mchorse.chameleon.animation.ActionPlayback.Fade;
import mchorse.chameleon.lib.ChameleonModel;
import mchorse.chameleon.lib.data.animation.Animation;
import mchorse.chameleon.lib.data.animation.Animations;
import mchorse.chameleon.lib.data.model.Model;
import mchorse.chameleon.metamorph.ChameleonMorph;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Animator class
 * 
 * This class is responsible for applying currently running actions onto 
 * morph (more specifically onto an armature).
 */
@SideOnly(Side.CLIENT)
public class Animator
{
    /* Actions */
    public ActionPlayback idle;
    public ActionPlayback running;
    public ActionPlayback sprinting;
    public ActionPlayback crouching;
    public ActionPlayback crouchingIdle;
    public ActionPlayback swimming;
    public ActionPlayback swimmingIdle;
    public ActionPlayback flying;
    public ActionPlayback flyingIdle;
    public ActionPlayback riding;
    public ActionPlayback ridingIdle;
    public ActionPlayback dying;
    public ActionPlayback falling;
    public ActionPlayback sleeping;

    public ActionPlayback jump;
    public ActionPlayback swipe;
    public ActionPlayback hurt;
    public ActionPlayback land;
    public ActionPlayback shoot;
    public ActionPlayback consume;

    /* Syncable action */
    public ActionPlayback animation;

    /* Action pipeline properties */
    public ActionPlayback active;
    public ActionPlayback lastActive;
    public List<ActionPlayback> actions = new ArrayList<ActionPlayback>();

    public double prevX = Float.MAX_VALUE;
    public double prevZ = Float.MAX_VALUE;
    public double prevMY;

    /* States */
    public boolean wasOnGround = true;
    public boolean wasShooting = false;
    public boolean wasConsuming = false;

    public ChameleonMorph morph;

    public Animator(ChameleonMorph morph)
    {
        this.morph = morph;
        this.refresh();
    }

    public void refresh() {
        ActionsConfig actions = this.morph.actions;

        this.idle = createActionWithCheck(this.idle, actions, "idle", true);
        this.running = createActionWithCheck(this.running, actions, "running", true);
        this.sprinting = createActionWithCheck(this.sprinting, actions, "sprinting", true);
        this.crouching = createActionWithCheck(this.crouching, actions, "crouching", true);
        this.crouchingIdle = createActionWithCheck(this.crouchingIdle, actions, "crouching_idle", true);
        this.swimming = createActionWithCheck(this.swimming, actions, "swimming", true);
        this.swimmingIdle = createActionWithCheck(this.swimmingIdle, actions, "swimming_idle", true);
        this.flying = createActionWithCheck(this.flying, actions, "flying", true);
        this.flyingIdle = createActionWithCheck(this.flyingIdle, actions, "flying_idle", true);
        this.riding = createActionWithCheck(this.riding, actions, "riding", true);
        this.ridingIdle = createActionWithCheck(this.ridingIdle, actions, "riding_idle", true);
        this.dying = createActionWithCheck(this.dying, actions, "dying", false);
        this.falling = createActionWithCheck(this.falling, actions, "falling", true);
        this.sleeping = createActionWithCheck(this.sleeping, actions, "sleeping", true);
        this.swipe = createActionWithCheck(this.swipe, actions, "swipe", false);
        this.jump = createActionWithCheck(this.jump, actions, "jump", false, 2);
        this.hurt = createActionWithCheck(this.hurt, actions, "hurt", false, 3);
        this.land = createActionWithCheck(this.land, actions, "land", false);
        this.shoot = createActionWithCheck(this.shoot, actions, "shoot", true);
        this.consume = createActionWithCheck(this.consume, actions, "consume", true);
        this.animation = createActionWithCheck(this.animation, actions, "animation", false);
    }

    private ActionPlayback createActionWithCheck(ActionPlayback old, ActionsConfig actions, String actionKey, boolean loop) {
        return createActionWithCheck(old, actions, actionKey, loop, 1);
    }

    private ActionPlayback createActionWithCheck(ActionPlayback old, ActionsConfig actions, String actionKey, boolean loop, int priority) {
        String nullString = "\"null\"";
        String actionName = (actions.getConfig(actionKey).toNBT()).toString();
        if (!actionName.equals(nullString)) {
            return this.createAction(old, actions.getConfig(actionKey), loop, priority);
        } else {
            return this.createAction(old, actions.getConfig(null), false, priority);
        }
    }

    /**
     * Create an action with default priority
     */
    public ActionPlayback createAction(ActionPlayback old, ActionConfig config, boolean looping)
    {
        return this.createAction(old, config, looping, 1);
    }

    /**
     * Create an action playback based on given arguments. This method
     * is used for creating actions so it was easier to tell which
     * actions are missing. Beside that, you can pass an old action so
     * in morph merging situation it wouldn't interrupt animation.
     */
    public ActionPlayback createAction(ActionPlayback old, ActionConfig config, boolean looping, int priority)
    {
        ChameleonModel model = this.morph.getModel();
        Animations animations = model == null ? null : model.animations;

        if (animations == null)
        {
            return null;
        }

        Animation action = animations.get(config.name);

        /* If given action is missing, then omit creation of ActionPlayback */
        if (action == null)
        {
            return null;
        }

        /* If old is the same, then there is no point creating a new one */
        if (old != null && old.action == action)
        {
            old.config = config;
            old.setSpeed(1);

            return old;
        }

        return new ActionPlayback(action, config, looping, priority);
    }

    /**
     * Update animator. This method is responsible for updating action 
     * pipeline and also change current actions based on entity's state.
     */
    public void update(EntityLivingBase target)
    {
        /* Fix issue with morphs sudden running action */
        if (this.prevX == Float.MAX_VALUE)
        {
            this.prevX = target.posX;
            this.prevZ = target.posZ;
        }

        this.controlActions(target);

        /* Update primary actions */
        if (this.active != null)
        {
            this.active.update();
        }

        if (this.lastActive != null)
        {
            this.lastActive.update();
        }

        /* Update secondary actions */
        Iterator<ActionPlayback> it = this.actions.iterator();

        while (it.hasNext())
        {
            ActionPlayback action = it.next();

            action.update();

            if (action.finishedFading() && action.isFadingModeOut())
            {
                action.stopFade();
                it.remove();
            }
        }
    }

    /**
     * This method is designed specifically to isolate any controlling 
     * code (i.e. the ones that is responsible for switching between 
     * actions).
     */
    protected void controlActions(EntityLivingBase target)
    {
        double dx = target.posX - this.prevX;
        double dz = target.posZ - this.prevZ;
        boolean creativeFlying = target instanceof EntityPlayer && ((EntityPlayer) target).capabilities.isFlying;
        boolean wet = target.isInWater();
        final float threshold = creativeFlying ? 0.1F : (wet ? 0.025F : 0.01F);
        boolean moves = Math.abs(dx) > threshold || Math.abs(dz) > threshold;

        if (target.getHealth() <= 0)
        {
            this.setActiveAction(this.dying);
        }
        else if (target.isPlayerSleeping())
        {
            this.setActiveAction(this.sleeping);
        }
        else if (wet)
        {
            this.setActiveAction(!moves ? this.swimmingIdle : this.swimming);
        }
        else if (target.isRiding())
        {
            Entity riding = target.getRidingEntity();
            moves = Math.abs(riding.posX - this.prevX) > threshold || Math.abs(riding.posZ - this.prevZ) > threshold;

            this.prevX = riding.posX;
            this.prevZ = riding.posZ;
            this.setActiveAction(!moves ? this.ridingIdle : this.riding);
        }
        else if (creativeFlying || target.isElytraFlying())
        {
            this.setActiveAction(!moves ? this.flyingIdle : this.flying);
        }
        else
        {
            if (target.isSneaking())
            {
                this.setActiveAction(!moves ? this.crouchingIdle : this.crouching);
            }
            else if (!target.onGround && target.motionY < 0 && target.fallDistance > 1.25)
            {
                this.setActiveAction(this.falling);
            }
            else if (target.isSprinting() && this.sprinting != null)
            {
                this.setActiveAction(this.sprinting);
            }
            else
            {
                this.setActiveAction(!moves ? this.idle : this.running);
            }

            if (target.onGround && !this.wasOnGround && !target.isSprinting() && this.prevMY < -0.5)
            {
                this.addAction(this.land);
            }
        }

        if (!target.onGround && this.wasOnGround && Math.abs(target.motionY) > 0.2F)
        {
            this.addAction(this.jump);
            this.wasOnGround = false;
        }

        /* Bow and consumables */
        boolean shooting = this.wasShooting;
        boolean consuming = this.wasConsuming;
        ItemStack stack = target.getHeldItemMainhand();

        if (!stack.isEmpty())
        {
            if (target.getItemInUseCount() > 0)
            {
                EnumAction action = stack.getItemUseAction();

                if (action == EnumAction.BOW)
                {
                    if (!this.actions.contains(this.shoot))
                    {
                        this.addAction(this.shoot);
                    }

                    this.wasShooting = true;
                }
                else if (action == EnumAction.DRINK || action == EnumAction.EAT)
                {
                    if (!this.actions.contains(this.consume))
                    {
                        this.addAction(this.consume);
                    }

                    this.wasConsuming = true;
                }
            }
            else
            {
                this.wasShooting = false;
                this.wasConsuming = false;
            }
        }
        else
        {
            this.wasShooting = false;
            this.wasConsuming = false;
        }

        if (shooting && !this.wasShooting && this.shoot != null)
        {
            this.shoot.fadeOut();
        }

        if (consuming && !this.wasConsuming && this.consume != null)
        {
            this.consume.fadeOut();
        }

        if (target.hurtTime == target.maxHurtTime - 1)
        {
            this.addAction(this.hurt);
        }

        if (target.isSwingInProgress && target.swingProgress == 0 && !target.isPlayerSleeping())
        {
            this.addAction(this.swipe);
        }

        this.prevX = target.posX;
        this.prevZ = target.posZ;
        this.prevMY = target.motionY;

        this.wasOnGround = target.onGround;
    }

    /**
     * Set current active (primary) action 
     */
    public void setActiveAction(ActionPlayback action)
    {
        if (this.active == action || action == null)
        {
            return;
        }

        if (this.active != null && action.priority < this.active.priority)
        {
            return;
        }

        if (this.active != null)
        {
            this.lastActive = this.active;
        }

        this.active = action;
        this.active.reset();
        this.active.fadeIn();
    }

    /**
     * Add an additional secondary action to the playback 
     */
    public void addAction(ActionPlayback action)
    {
        if (action == null)
        {
            return;
        }

        if (this.actions.contains(action))
        {
            action.reset();

            return;
        }

        action.reset();
        action.fadeIn();
        this.actions.add(action);
    }

    /**
     * Apply currently running action pipeline onto given armature
     */
    public void applyActions(EntityLivingBase target, Model armature, float partialTicks)
    {
        if (this.animation != null && this.morph.isActionPlayer)
        {
            float ticks = this.morph.animation.getFactor(partialTicks) * this.morph.animation.duration;
            boolean doFade = this.morph.lastAnimAction != null;

            if (doFade)
            {
                this.applyAnimation(this.morph.lastAnimAction, target, armature, ticks, Fade.OUT);
                this.applyAnimation(this.animation, target, armature, ticks, Fade.IN);
            }
            else
            {
                this.applyAnimation(this.animation, target, armature, ticks, Fade.FINISHED);
            }

            return;
        }

        if (this.lastActive != null && this.active.isFading())
        {
            this.lastActive.apply(target, armature, partialTicks, 1F, false);
        }

        if (this.active != null)
        {
            float fade = this.active.isFading() ? this.active.getFadeFactor(partialTicks) : 1F;

            this.active.apply(target, armature, partialTicks, fade, false);
        }

        for (ActionPlayback action : this.actions)
        {
            if (action.isFading())
            {
                action.apply(target, armature, partialTicks, action.getFadeFactor(partialTicks), true);
            }
            else
            {
                action.apply(target, armature, partialTicks, 1F, true);
            }
        }
    }

    public void applyAnimation(ActionPlayback animation, EntityLivingBase target, Model armature, float ticks, Fade fade)
    {
        float progress = ticks * Math.abs(animation.config.speed);
        float fadeFactor = animation.config.fade < 0.0001F ? 1F : MathHelper.clamp(progress / animation.config.fade, 0F, 1F);

        if (fade == Fade.FINISHED)
        {
            fadeFactor = 1F;
        }
        else if (fade == Fade.OUT)
        {
            fadeFactor = fadeFactor >= 0.999F ? 0F : 1F;
        }

        if (fadeFactor < 0.0001F)
        {
            return;
        }

        progress += animation.config.clamp ? animation.config.tick : 0F;

        if (animation.config.speed < 0)
        {
            progress = (float) (animation.action.length) - progress;
        }

        animation.apply(target, armature, animation.config.speed == 0F ? 0F : progress / animation.config.speed, fadeFactor, false);
    }
}