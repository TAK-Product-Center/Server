import { Component, Input, OnInit, Inject} from "@angular/core";
import { CommonModule } from "@angular/common";
import {MatButtonModule} from '@angular/material/button';
import { MatDialogModule, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { FormsModule } from '@angular/forms'; // Import FormsModule
import { DataService } from "../../../data.service";


@Component({
  selector: 'app-add-outgoing-connection-modal',
  imports: [CommonModule, MatButtonModule, MatDialogModule, FormsModule],
  templateUrl: './add-annotation-modal.html',
  styleUrl: './add-annotation-modal.css'
})
export class AddAnnotationModal {    

  certificateFile = null

  stringId : any = undefined
  editExisting : boolean = false;
  editorTitle : string = "Edit";
  submitInProgress : boolean = false;
  federation : any = {}

  existingName = () => {
    let exists = this.dataService.getFederationDataForNodes()
      // ignore self
      .filter((f: any) => f.node_id !== this.federation.node_id)
      // map to just an array of existing node names
      .map((f: any) => f.stringId)
      .includes(this.federation.stringId)

      return exists
  }

  constructor(@Inject(MAT_DIALOG_DATA) public data: any, private dataService: DataService) {
    this.federation = JSON.parse(JSON.stringify(data.federation))
    this.editExisting = true;
  }

  // Utility function to convert hex to RGB
  private hexToRgb(hex: string): { r: number, g: number, b: number } | null {
    if (!hex || typeof hex !== 'string') {
      return null;
    }

    // Expand shorthand form (e.g. "03F") to full form (e.g. "0033FF")
    const shorthandRegex = /^#?([a-f\d])([a-f\d])([a-f\d])$/i;
    hex = hex.replace(shorthandRegex, function(m, r, g, b) {
      return r + r + g + g + b + b;
    });

    const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
    return result ? {
      r: parseInt(result[1], 16),
      g: parseInt(result[2], 16),
      b: parseInt(result[3], 16)
    } : null;
  }

  // Function to determine if a color is dark or light and set fontColor
  private updateFontColor(): void {
    const rgb = this.hexToRgb(this.federation.bgColor);

    if (rgb) {
      // Calculate perceived brightness (luminance) using a common formula
      // (ITU-R BT.709 standard is widely used)
      // The values are typically weighted because green is perceived brighter than red, and red brighter than blue.
      const luminance = (0.2126 * rgb.r + 0.7152 * rgb.g + 0.0722 * rgb.b);

      // A common threshold is around 128 out of 255
      if (luminance < 128) {
        this.federation.fontColor = '#FFFFFF'; // Dark background, use white text
      } else {
        this.federation.fontColor = '#000000'; // Light background, use black text
      }
    } else {
      // Fallback if color is invalid (e.g., initial empty string)
      this.federation.fontColor = '#000000';
    }
  }

  // This method will be called when the color picker's value changes
  onBgColorChange(): void {
    this.updateFontColor();
  }

  onSave(federation : any) {
    if (!federation.outgoing_uuid) {
      federation.outgoing_uuid = this.create_UUID()
    }
    federation.outgoingName = federation.stringId + '_' + federation.outgoing_uuid;
    federation.name = federation.outgoingName;
    federation.bgColor = federation.bgColor;

    if (this.data.onSave) {
      this.data.onSave(federation);
    }
  }

  onCancel() {
    if (this.data.onCancel) {
      this.data.onCancel(this.editExisting);
    }
  }

  create_UUID(){
    var dt = new Date().getTime();
    var uuid = 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, function(c) {
        var r = (dt + Math.random()*16)%16 | 0;
        dt = Math.floor(dt/16);
        return (c=='x' ? r :(r&0x3|0x8)).toString(16);
    });
    return uuid;
  }
}
