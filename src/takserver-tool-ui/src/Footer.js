import './Footer.css';
import React, { useEffect } from 'react';

function Footer() {
    const [version, setVersion] = React.useState("");
    const [nodeId, setNodeId] = React.useState("");

    // Only get Version and NodeID once on page load
    useEffect(() => {
        fetch('/Marti/api/version')
            .then(response => response.text())
            .then(data => {
                setVersion(data);
        });
        fetch('/Marti/api/node/id')
            .then(response => response.text())
            .then(data => {
                setNodeId(data);
        });
    },[])
    return (
        <div className='TableFooter'>
            <table>
                <tr>
	                <p><center>
                    {"TAK Server " + version}
                    </center></p> 
                    <p><center>
                    <i>{"Node ID: " + nodeId}</i> 
                    </center></p> 
                    </tr>
            </table>
        </div>
    )
}

export default Footer;