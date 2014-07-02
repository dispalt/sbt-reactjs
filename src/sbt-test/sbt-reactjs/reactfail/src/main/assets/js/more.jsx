/** @jsx React.DOM */
var MoreMessage = React.createClass({
  render: function() {
    return <div>Hello {this.props.name}</div>;
  }
});

React.renderComponent(<MoreMessage name="John" />, mountNode);