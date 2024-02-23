/*
 * Copyright (c) 2017 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.goodbytes.xmpp.xep0363;

import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.component.AbstractComponent;
import org.xmpp.packet.IQ;
import org.xmpp.packet.PacketError;

import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * A XMPP component that implements XEP-0363.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363</a>
 */
public class Component extends AbstractComponent
{
    // Earlier namespace, used before v0.3.0 of XEP-0363
    public final static String NAMESPACE_EXP = "urn:xmpp:http:upload";

    // Namespace from version 0.3.0 onwards.
    public final static String NAMESPACE = "urn:xmpp:http:upload:0";

    private static final Logger Log = LoggerFactory.getLogger( Component.class );
    private final String name;
    private final String endpoint;

    /**
     * Instantiates a new component.
     *
     * The URL that's provided in the second argument is used as the base URL for all client interaction. More
     * specifically, this value is used to generate the slot filenames. The URL should be accessible for end-users.
     *
     * @param name     The component name (cannot be null or an empty String).
     * @param endpoint The base URL for HTTP interaction (cannot be null).
     */
    public Component( String name, URL endpoint )
    {
        super();

        if ( name == null || name.trim().isEmpty() )
        {
            throw new IllegalArgumentException( "Argument 'name' cannot be null or an empty String." );
        }
        if ( endpoint == null )
        {
            throw new IllegalArgumentException( "Argument 'endpoint' cannot be null." );
        }
        this.name = name.trim();
        this.endpoint = endpoint.toExternalForm();
    }

    @Override
    public String getDescription()
    {
        return "HTTP File Upload, an implementation of XEP-0363, supporting exchange of files between XMPP entities.";
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    protected String discoInfoIdentityCategory()
    {
        return "store"; // TODO: the XEP example reads 'store' but I'm unsure if this is a registered type.
    }

    @Override
    protected String discoInfoIdentityCategoryType()
    {
        return "file"; // TODO: the XEP example reads 'file' but I'm unsure if this is a registered type.
    }

    @Override
    protected String[] discoInfoFeatureNamespaces()
    {
        return new String[] { NAMESPACE, NAMESPACE_EXP };
    }

    @Override
    protected IQ handleDiscoInfo( IQ iq )
    {
        final IQ response = super.handleDiscoInfo( iq );

        // Add service configuration / preconditions if these exist.
        if ( SlotManager.getInstance().getMaxFileSize() > 0 )
        {
            final Element configForm = response.getChildElement().addElement( "x", "jabber:x:data" );
            configForm.addAttribute( "type", "result" );
            configForm.addElement( "field" ).addAttribute( "var", "FORM_TYPE" ).addAttribute( "type", "hidden" ).addElement( "value" ).addText( NAMESPACE );
            configForm.addElement( "field" ).addAttribute( "var", "max-file-size" ).addElement( "value" ).addText( Long.toString( SlotManager.getInstance().getMaxFileSize() ) );
        }

        return response;
    }

    @Override
    protected IQ handleIQGet( IQ iq ) throws Exception
    {
        final Element request = iq.getChildElement();
        final Collection<String> namespaces = Arrays.asList( NAMESPACE, NAMESPACE_EXP );
        if ( !namespaces.contains( request.getNamespaceURI() ) || !request.getName().equals( "request" ) )
        {
            return null;
        }
        final boolean isPre030Style = NAMESPACE_EXP.equals( request.getNamespaceURI() );

        Log.info( "Entity '{}' tries to obtain slot.", iq.getFrom() );
        String fileName = null;
        if ( request.attributeValue( "filename" ) != null && !request.attributeValue( "filename" ).trim().isEmpty() )
        {
            fileName = request.attributeValue( "filename" ).trim();
        }

        if ( request.element( "filename" ) != null && !request.element( "filename" ).getTextTrim().isEmpty() )
        {
            fileName = request.element( "filename" ).getTextTrim();
        }

        if ( fileName == null )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        // TODO validate the file name (path traversal, etc).

        String size = null;
        if ( request.attributeValue( "size" ) != null && !request.attributeValue( "size" ).isEmpty() )
        {
            size = request.attributeValue( "size" ).trim();
        }

        if ( request.element( "size" ) != null && !request.element( "size" ).getTextTrim().isEmpty() )
        {
            size = request.element( "size" ).getTextTrim();
        }

        if ( size == null )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        final long fileSize;
        try
        {
            fileSize = Long.parseLong( size );
        }
        catch ( NumberFormatException e )
        {
            final IQ response = IQ.createResultIQ( iq );
            response.setError( PacketError.Condition.bad_request );
            return response;
        }

        String contentType = null;
        if (request.element( "content-type" ) != null)
        {
            contentType = request.element( "content-type" ).getTextTrim();
        }

        final SlotManager manager = SlotManager.getInstance();
        final Slot slot;
        try
        {
            slot = manager.getSlot( iq.getFrom(), fileName, fileSize, contentType );
        }
        catch ( TooLargeException ex )
        {
            final IQ response = IQ.createResultIQ( iq );
            final PacketError error = new PacketError( PacketError.Condition.not_acceptable, PacketError.Type.modify, "File too large. Maximum file size is " + ex.getMaximum() + " bytes." );
            error.getElement().addElement( "file-too-large", iq.getChildElement().getNamespaceURI() ).addElement( "max-file-size" ).addText( Long.toString( ex.getMaximum() ) );
            response.setError( error );
            return response;
        }

        final URL url = new URL( URI.create( endpoint + "/" + slot.getUuid() + "/" + fileName ).toASCIIString() );

        Log.info( "Entity '{}' obtained slot for '{}' ({} bytes): {}", iq.getFrom(), fileName, fileSize, url.toExternalForm() );

        final IQ response = IQ.createResultIQ( iq );
        final Element slotElement = response.setChildElement( "slot", iq.getChildElement().getNamespaceURI() );
        if ( isPre030Style )
        {
            slotElement.addElement( "put" ).setText( url.toExternalForm() );
            slotElement.addElement( "get" ).setText( url.toExternalForm() );
        } else {
            slotElement.addElement( "put" ).addAttribute( "url", url.toExternalForm() );
            slotElement.addElement( "get" ).addAttribute( "url", url.toExternalForm() );
        }
        return response;
    }
}
