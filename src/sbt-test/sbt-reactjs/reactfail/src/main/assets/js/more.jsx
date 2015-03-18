export var MoreMessage = React.createClass({
  render: function() {
    return <div>Hello {this.props.name}</div>;
  }
});

React.render(<MoreMessage name="John" />, mountNode);
