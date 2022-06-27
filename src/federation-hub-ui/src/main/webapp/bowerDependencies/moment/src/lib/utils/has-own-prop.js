/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
export default function hasOwnProp(a, b) {
    return Object.prototype.hasOwnProperty.call(a, b);
}
