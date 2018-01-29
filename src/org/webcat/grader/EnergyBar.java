/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2017-2018 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU Affero General Public License as published
 |  by the Free Software Foundation; either version 3 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details.
 |
 |  You should have received a copy of the GNU Affero General Public License
 |  along with Web-CAT; if not, see <http://www.gnu.org/licenses/>.
\*==========================================================================*/

package org.webcat.grader;

import org.webcat.core.User;
import org.webcat.woextensions.ECActionWithResult;
import static org.webcat.woextensions.ECActionWithResult.*;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSDictionary;
import com.webobjects.foundation.NSTimestamp;

// -------------------------------------------------------------------------
/**
 * TODO: place a real description here.
 *
 * @author
 * @author  Last changed by: $Author$
 * @version $Revision$, $Date$
 */
public class EnergyBar
    extends _EnergyBar
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new EnergyBar object.
     */
    public EnergyBar()
    {
        super();
    }


    //~ Constants .............................................................

    public static final int CHARGE_CONSUMED = 1;
    public static final int SUBMISSION_DENIED = 2;
    public static final int SUBMISSION_CLOSE_TO_DEADLINE = 3;
    public static final int FORCE_RECHARGE = 4;
    public static final int SUBMISSION_DENY_FAILED = 5;


    //~ Methods ...............................................................

    public static EnergyBar forAssignmentAndUser(
        final EOEditingContext context,
        final AssignmentOffering anAssignmentOffering,
        final User aUser)
    {
        EnergyBar bar = null;
        if (anAssignmentOffering.energyBarConfig() != null)
        {
            bar = uniqueObjectMatchingQualifier(context,
                assignmentOffering.is(anAssignmentOffering)
                .and(user.is(aUser)));
            if (bar == null)
            {
                bar = new ECActionWithResult<EnergyBar>()
                {
                    public EnergyBar action()
                    {
                        EnergyBar result = EnergyBar.create(ec,
                            anAssignmentOffering.energyBarConfig().numSlots(),
                            anAssignmentOffering.localInstance(ec),
                            aUser.localInstance(ec));
                        ec.saveChanges();
                        return result.localInstance(context);
                    }
                }.call();
            }
        }
        return bar;
    }

    public String userPresentableDescription()
    {
        return user() + "[" + assignmentOffering() + "]: " + charge();
    }

    public boolean hasEnergy()
    {
        reevaluateCharge();
        return charge() > 0;
    }

    public boolean isCloseToDeadline(NSTimestamp now)
    {
        if (charge() > 0 || hasEnergy() || rechargeStart() == null)
        {
            return false;
        }
        NSTimestamp deadline = assignmentOffering().dueDate();

        return now.timestampByAddingGregorianUnits(0, 0, 0, 0, 0, chargeRate())
            .after(deadline) && deadline.after(now);
    }

    public boolean consumeEnergyIfPossible(NSTimestamp atTime)
    {
        reevaluateCharge();
        if (charge() > 0)
        {
            setCharge(charge() - 1);
            if (rechargeStart() == null)
            {
                setRechargeStart(atTime);
            }
            return true;
        }
        return false;
    }

    public NSTimestamp timeOfNextCharge()
    {
        if (charge() < assignmentOffering().energyBarConfig().numSlots())
        {
            if (rechargeStart() == null)
            {
                throw new IllegalStateException(
                    "unexpected null rechargeStart for " +
                    userPresentableDescription());
            }
            int chargeRate = chargeRate();
            NSTimestamp nextRechargeTime = rechargeStart()
                .timestampByAddingGregorianUnits(0, 0, 0, 0, 0, chargeRate);
            if (rateExpiration() != null
                && nextRechargeTime.after(rateExpiration()))
            {
                int defaultRate =
                    assignmentOffering().energyBarConfig().rechargeTime();
                double fraction =
                    (rateExpiration().getTime() - rechargeStart().getTime())
                    / (chargeRate * 1000.0);
                nextRechargeTime = new NSTimestamp(
                    rateExpiration().getTime()
                    + (long)(defaultRate * (1.0 - fraction) * 1000.0));
            }
            return nextRechargeTime;
        }
        return null;
    }

    public int chargeRate()
    {
        int chargeRate = rate();
        if (chargeRate <= 0)
        {
            chargeRate = assignmentOffering().energyBarConfig().rechargeTime();
        }
        return chargeRate;
    }

    public void reevaluateCharge()
    {
        int maxCharge = assignmentOffering().energyBarConfig().numSlots();
        if (charge() == maxCharge)
        {
            setRechargeStart(null);
            return;
        }
        NSTimestamp now = new NSTimestamp();
        NSTimestamp nextRechargeTime = timeOfNextCharge();
        if (nextRechargeTime.after(now))
        {
            return;
        }
        setCharge(charge() + 1);
        setRechargeStart(nextRechargeTime);
        if ((rateExpiration() != null) &&
            (rateExpiration().before(nextRechargeTime)))
        {
            setRateExpiration(null);
            setRateRaw(null);
        }
        reevaluateCharge();
    }

    public EnergyBarEvent logEvent(int eventType)
    {
        return logEvent(eventType, null, null, null);
    }

    public EnergyBarEvent logEvent(int eventType, NSTimestamp eventTime)
    {
        return logEvent(eventType, eventTime, null, null);
    }

    public EnergyBarEvent logEvent(int eventType, Submission aSubmission)
    {
        EnergyBarEvent e = logEvent(eventType, null,
            aSubmission, aSubmission.assignmentOffering());
        e.setSubmission(aSubmission);
        return e;
    }

    public EnergyBarEvent logEvent(int eventType, AssignmentOffering assignment)
    {
        return logEvent(eventType, null, null, assignment);
    }

    private EnergyBarEvent logEvent(
        final int eventType,
        final NSTimestamp time,
        final Submission aSubmission,
        final AssignmentOffering assignment)
    {
        final EnergyBar myself = this;
        return call(new ECActionWithResult<EnergyBarEvent>()
        {
            public EnergyBarEvent action()
            {
                EnergyBar bar = myself.localInstance(ec);
                NSDictionary<?, ?> changes =
                    myself.changesFromCommittedSnapshot();
                bar.reapplyChangesFromDictionary(changes);
                bar.reevaluateCharge();
                Submission mySubmission = null;
                if (aSubmission != null)
                {
                    mySubmission = aSubmission.localInstance(ec);
                }
                AssignmentOffering myAssignment = null;
                if (assignment != null)
                {
                    myAssignment = assignment.localInstance(ec);
                }
                else if (mySubmission != null)
                {
                    myAssignment = mySubmission.assignmentOffering();
                }
                NSTimestamp myTime = time;
                if (myTime == null)
                {
                    if (mySubmission == null)
                    {
                        myTime = new NSTimestamp();
                    }
                    else
                    {
                        myTime = mySubmission.submitTime();
                    }
                }
                EnergyBarEvent event = EnergyBarEvent.create(ec,
                    bar.charge(), myTime, eventType, bar);
                event.setTimeOfNextCharge(bar.timeOfNextCharge());
                event.setSubmissionRelationship(mySubmission);
                event.setAssignmentOfferingRelationship(myAssignment);
                ec.saveChanges();
                return event.localInstance(myself.editingContext());
            }
        });
    }
}
