/*
 * Copyright (c) 2004-2013 Universidade do Porto - Faculdade de Engenharia
 * Laboratório de Sistemas e Tecnologia Subaquática (LSTS)
 * All rights reserved.
 * Rua Dr. Roberto Frias s/n, sala I203, 4200-465 Porto, Portugal
 *
 * This file is part of Neptus, Command and Control Framework.
 *
 * Commercial Licence Usage
 * Licencees holding valid commercial Neptus licences may use this file
 * in accordance with the commercial licence agreement provided with the
 * Software or, alternatively, in accordance with the terms contained in a
 * written agreement between you and Universidade do Porto. For licensing
 * terms, conditions, and further information contact lsts@fe.up.pt.
 *
 * European Union Public Licence - EUPL v.1.1 Usage
 * Alternatively, this file may be used under the terms of the EUPL,
 * Version 1.1 only (the "Licence"), appearing in the file LICENSE.md
 * included in the packaging of this file. You may not use this work
 * except in compliance with the Licence. Unless required by applicable
 * law or agreed to in writing, software distributed under the Licence is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF
 * ANY KIND, either express or implied. See the Licence for the specific
 * language governing permissions and limitations at
 * https://www.lsts.pt/neptus/licence.
 *
 * For more information please see <http://lsts.fe.up.pt/neptus>.
 *
 * Author: zp
 * Jun 27, 2013
 */
package pt.up.fe.dceg.neptus.plugins.alignment;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.Vector;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import pt.up.fe.dceg.neptus.console.ConsoleLayout;
import pt.up.fe.dceg.neptus.console.events.ConsoleEventMainSystemChange;
import pt.up.fe.dceg.neptus.i18n.I18n;
import pt.up.fe.dceg.neptus.imc.AlignmentState;
import pt.up.fe.dceg.neptus.imc.EntityActivationState;
import pt.up.fe.dceg.neptus.imc.EntityParameter;
import pt.up.fe.dceg.neptus.imc.EntityState;
import pt.up.fe.dceg.neptus.imc.EstimatedState;
import pt.up.fe.dceg.neptus.imc.QueryEntityActivationState;
import pt.up.fe.dceg.neptus.imc.SetEntityParameters;
import pt.up.fe.dceg.neptus.mp.Maneuver.SPEED_UNITS;
import pt.up.fe.dceg.neptus.mp.ManeuverLocation;
import pt.up.fe.dceg.neptus.mp.templates.PlanCreator;
import pt.up.fe.dceg.neptus.plugins.NeptusProperty;
import pt.up.fe.dceg.neptus.plugins.Popup;
import pt.up.fe.dceg.neptus.plugins.SimpleSubPanel;
import pt.up.fe.dceg.neptus.plugins.update.IPeriodicUpdates;
import pt.up.fe.dceg.neptus.types.coord.LocationType;
import pt.up.fe.dceg.neptus.types.mission.plan.PlanType;
import pt.up.fe.dceg.neptus.util.ImageUtils;
import pt.up.fe.dceg.neptus.util.comm.manager.imc.EntitiesResolver;

import com.google.common.eventbus.Subscribe;

/**
 * This panel will allow monitoring and alignment of some IMUs
 * @author zp
 */
@Popup(accelerator=KeyEvent.VK_I, pos=Popup.POSITION.CENTER, height=300, width=300, name="Dead reckoning alignment")
public class ImuAlignmentPanel extends SimpleSubPanel implements IPeriodicUpdates {

    private static final long serialVersionUID = -1330079540844029305L;
    protected JToggleButton enableImu;
    protected JButton doAlignment;
    protected JEditorPane status;

    // used to activate and deactivate payload
    @NeptusProperty(name="IMU Entity Label")
    public String imuEntity = "IMU";

    @NeptusProperty(name="Navigation Entity Label")
    public String navEntity = "Navigation";

    @NeptusProperty(name="Square Side Length")
    public double squareSideLength = 50;

    @NeptusProperty(name="Alignment Speed")
    public double alignSpeed = 1.25;

    @NeptusProperty(name="Alignment Speed Units")
    public SPEED_UNITS alignSpeedUnits = SPEED_UNITS.METERS_PS;

    protected ImageIcon greenLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_green.png");
    protected ImageIcon redLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_red.png");
    protected ImageIcon grayLed = ImageUtils.getIcon("pt/up/fe/dceg/neptus/plugins/alignment/led_none.png");

    public ImuAlignmentPanel(ConsoleLayout console) {
        super(console);        
        setLayout(new BorderLayout(3, 3));
        removeAll();
        JPanel top = new JPanel(new GridLayout(1, 0));
        enableImu = new JToggleButton(I18n.text("Enable IMU"), grayLed, false);
        enableImu.setToolTipText(I18n.text("Waiting for first IMU alignment state"));
        enableImu.setEnabled(false);
        top.add(enableImu);

        enableImu.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                toggleImu(enableImu.isSelected());                
            }
        });
        
        
        
        doAlignment = new JButton(I18n.text("Do Alignment"));
        top.add(doAlignment);
        status = new JEditorPane("text/html", updateState());
        doAlignment.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                doAlignment();
            }
        });
        add(top, BorderLayout.NORTH);
        add(status, BorderLayout.CENTER);
    }

    @Override
    public long millisBetweenUpdates() {        
        return 1000;
    }

    @Override
    public boolean update() {
        int imuId = EntitiesResolver.resolveId(getMainVehicleId(), imuEntity);
        if (imuId != -1) {
            QueryEntityActivationState qeas = new QueryEntityActivationState();
            qeas.setDstEnt(imuId);
            send(qeas);
        }
       
        status.setText(updateState());
        return true;
    }

    public String updateState() {

        AlignmentState alignState = getState().lastAlignmentState();
        EntityState imuState = (EntityState) getState().get(EntityState.ID_STATIC, EntitiesResolver.resolveId(getMainVehicleId(), imuEntity));
        EntityActivationState imuActivationState = (EntityActivationState) getState().get(EntityActivationState.ID_STATIC, EntitiesResolver.resolveId(getMainVehicleId(), imuEntity));
        
        if (imuState == null) 
            enableImu.setEnabled(false);
        else if (imuActivationState != null){
            switch (imuActivationState.getState()) {
                case ACTIVE:
                    enableImu.setSelected(true);
                    break;

                default:
                    enableImu.setSelected(false);
                    break;
            }
        }

        if (alignState == null)
            return "<html><h1>"+I18n.text("Waiting for IMU alignment state")+"</h1></html>";
        switch(alignState.getState()) {
            case ALIGNED:
                enableImu.setIcon(greenLed);
                enableImu.setToolTipText(I18n.text("IMU aligned. Vehicle can be used in dead-reckoning mode."));
                enableImu.setEnabled(true);
                return "<html><h1><font color='red'>"+I18n.text("IMU aligned")+"</font></h1>"
                +"<p>"+I18n.text("Vehicle can now be used to execute dead reckoning missions.")+"</p>"
                +"</html>";
            case NOT_ALIGNED:
                enableImu.setIcon(redLed);
                enableImu.setEnabled(true);
                enableImu.setToolTipText(I18n.text("IMU is not aligned"));
                return "<html><h1><font color='red'>"+I18n.text("IMU not aligned")+"</font></h1>"
                +"<p>"+I18n.text("In order to execute dead reckoning missions, IMU must first be aligned.")+"</p>"                
                +"</html>";
            default:
                enableImu.setIcon(grayLed);
                enableImu.setEnabled(false);
                enableImu.setToolTipText(I18n.text("IMU cannot be aligned on "+alignState.getSourceName()));
                enableImu.setSelected(false);
                return "<html><h1><font color='red'>"+I18n.text("IMU cannot be aligned")+"</font></h1>"
                +"<p>"+I18n.text("This vehicle does not support IMU alignment.")+"</p>"                
                +"</html>";
        }
    }

    public void doAlignment() {

        int opt = JOptionPane.showConfirmDialog(getConsole(), "<html><h2>Alignment Procedure</h2>"
                +"To do IMU alignment, the vehicle must do straigth segments at the surface.<br>"
                +"<b>Do you want me to create a plan at surface for you?</b>");


        if (opt == JOptionPane.YES_OPTION) {
            EstimatedState lastState = getState().lastEstimatedState();

            PlanCreator pc = new PlanCreator(getConsole().getMission());
            if (lastState != null && lastState.getLat() != 0) {
                LocationType loc = new LocationType(
                        Math.toDegrees(lastState.getLat()), 
                        Math.toDegrees(lastState.getLon()));
                loc.translatePosition(lastState.getX(), lastState.getY(), 0);
                pc.setLocation(loc);
            }
            else
            {
                pc.setLocation(new LocationType(getConsole().getMission().getHomeRef()));
            }
            pc.setSpeed(alignSpeed, alignSpeedUnits);
            pc.setZ(0, ManeuverLocation.Z_UNITS.DEPTH);

            pc.addGoto(null);
            pc.move(squareSideLength, 0);
            pc.addGoto(null);
            pc.move(0, -squareSideLength);
            pc.addGoto(null);
            pc.move(-squareSideLength, 0);
            pc.addGoto(null);
            pc.move(0, squareSideLength);
            pc.addGoto(null);
            PlanType pt = pc.getPlan();
            pt.setId("alignment_template");
            pt.setVehicle(getConsole().getMainSystem());
            getConsole().getMission().addPlan(pt);
            getConsole().setPlan(pt);
            getConsole().warnMissionListeners();
            getConsole().getMission().save(true);                        

        }
    }

    public void toggleImu(boolean newState) {
        Vector<EntityParameter> params = new Vector<>();
        params.add(new EntityParameter("Active", ""+newState));
        SetEntityParameters m = new SetEntityParameters(imuEntity, params);
        send(m);
    }

    @Override
    public void initSubPanel() {
        
    }

    @Subscribe
    public void on(ConsoleEventMainSystemChange evt) {
        update();
    }
    
    @Override
    public void cleanSubPanel() {
        // TODO Auto-generated method stub

    }

}
