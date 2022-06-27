/*******************************************************************************
 * Copyright (c) 2019 Raytheon BBN Technologies.
 *******************************************************************************/
import { Moment } from './constructor';

export function clone () {
    return new Moment(this);
}
