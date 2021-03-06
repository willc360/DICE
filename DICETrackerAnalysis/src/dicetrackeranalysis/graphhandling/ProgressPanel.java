/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * ProgressPanel.java
 *
 * Created on 19-Jun-2012, 3:55:03 PM
 */

package dicetrackeranalysis.graphhandling;

/**
 *
 * @author lee
 */
public class ProgressPanel extends javax.swing.JPanel {

    private int maxValue;

    /** Creates new form ProgressPanel */
    public ProgressPanel(String description, int maxValue) {
        initComponents();

        this.maxValue = maxValue;
        this.description.setText(description);
        this.progressBar.setMaximum(maxValue);
        this.progressBar.setMinimum(0);
        this.progressBar.setValue(0);
    }

    public void setValue(int value) {
        if (value <= this.maxValue && value >= 0)
            this.progressBar.setValue(value);
    }

    public int getValue() {
        return this.progressBar.getValue();
    }

    public void setPortion(double portion) {
        this.progressBar.setValue((int)(portion * this.maxValue));
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        description = new javax.swing.JLabel();
        progressBar = new javax.swing.JProgressBar();

        jLabel1.setText("In Progress: ");

        description.setText("jLabel2");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(progressBar, javax.swing.GroupLayout.DEFAULT_SIZE, 376, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(description)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(description))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(progressBar, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel description;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JProgressBar progressBar;
    // End of variables declaration//GEN-END:variables

}
